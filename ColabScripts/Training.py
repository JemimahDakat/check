import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, Dataset
from torchvision import models, transforms
import cv2
import numpy as np
import os
import glob
import random
from google.colab import drive

# set the path for the cleaned dataset
DATASET_PATH = '/content/sorted_dataset'

#temporal window. LSTM processes 30 sequential frames per video.
SEQ_LENGTH = 30

# the number of data samples processed simultaneously during a single pass.
BATCH_SIZE = 32

# Check for NVIDIA CUDA availability to route tensor operations to the GPU for hardware acceleration.
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
print(f"Computational Device: {device}")

print("PHASE 1:")

# import ResNet-18 that is pretrained.
resnet = models.resnet18(pretrained=True)

# remove the linear layer
#  converts CNN from an image classifier into a spatial feature extractor.
#  now outputs a 512-dimensional feature vector instead of class probabilities
feature_extractor = nn.Sequential(*list(resnet.children())[:-1]).to(device)

# set the CNN to eval mode-  freezes BatchNorm and Dropout layers,
# preventing the pre-trained weights from deleting/ updating during extraction
feature_extractor.eval()

# Define the image preprocessing pipeline for input tensors.
transform = transforms.Compose([
    transforms.ToPILImage(),
    # downsize frames to 224x224 pixels to match ResNet's required input
    transforms.Resize((224, 224)),
    # Convert pixel matrices to PyTorch Tensors and scale values to a [0.0, 1.0] range.
    transforms.ToTensor(),
    # Normalise the RGB channels using the statistical mean and standard deviation of ImageNet.
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])

features_data = [] # Array to store tuples of (Tensor_Vector, Integer_Label)
classes = ['real', 'fake']

for label_idx, class_name in enumerate(classes):
    folder_path = os.path.join(DATASET_PATH, class_name)
    video_files = glob.glob(os.path.join(folder_path, "*.mp4"))

    # set a static random seed to ensure reproducible stratified sampling across executions.
    random.seed(42)
    random.shuffle(video_files)

    # 500:1500 (1:3) dataset subset to prevent RAM allocation errors
    #class imbalance ratio for the loss function calculation
    if class_name == 'real':
        video_files = video_files[:500]
    elif class_name == 'fake':
        video_files = video_files[:1500]

    print(f"Processing class '{class_name}' ({len(video_files)} samples)...")

    for i, video_path in enumerate(video_files):
        try:
            # Initialise OpenCV video capture stream
            cap = cv2.VideoCapture(video_path)
            total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

            frames_list = []

            # work out  frame sampling interval so that the 30 extracted framesare spaced evenly across the entire duration of the video file.
            step = max(total_frames // SEQ_LENGTH, 1)

            for j in range(SEQ_LENGTH):
                # Seek to the specific frame index.
                cap.set(cv2.CAP_PROP_POS_FRAMES, j * step)
                ret, frame = cap.read()
                if ret:
                    # convert from OpenCV's default BGR color space to standard RGB.
                    frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                    frames_list.append(transform(frame))
            cap.release()

            if len(frames_list) == SEQ_LENGTH:
                # Concatenate the list of 3D image tensors into a single 4D batch tensor.
                # Shape becomes: (30, 3, 224, 224) representing (Frames, Channels, Height, Width).
                batch = torch.stack(frames_list).to(device)

                # Context manager to disable gradient tracking, significantly reducing VRAM consumption.
                with torch.no_grad():
                    # Execute forward pass through ResNet-18 to perform dimensionality reduction.
                    features = feature_extractor(batch)

                    # Flatten the spatial dimensions to output a 2D sequence array.
                    # Final shape: (30, 512). 30 frames, each represented by a 512-float vector.
                    features = features.view(SEQ_LENGTH, -1).cpu().numpy()

                features_data.append((features, label_idx))

        except Exception:
            pass # Silently bypass unreadable or corrupted video chunks.

        if i > 0 and i % 100 == 0:
            print(f"      Extracted {i}/{len(video_files)}")

print(f"Extraction Complete. Valid Samples: {len(features_data)}")

print("LSTM TRAINING")

# Custom Dataset class to interface with PyTorch's DataLoader.
# Maps the extracted feature vectors to their corresponding binary labels.
class VectorDataset(Dataset):
    def __init__(self, data): self.data = data
    def __len__(self): return len(self.data)
    def __getitem__(self, i):
        return torch.tensor(self.data[i][0]), self.data[i][1]

# starts an iterable that yields batched, shuffled tensors for training.
train_loader = DataLoader(VectorDataset(features_data), batch_size=BATCH_SIZE, shuffle=True)

class RNNClassifier(nn.Module):
    def __init__(self):
        super().__init__()
        # LSTM recurrent layer to evaluate temporal sequential data
        # Maps the 512-dimensional CNN input to a 64-dimensional hidden memory state.
        self.lstm = nn.LSTM(input_size=512, hidden_size=64, num_layers=1, batch_first=True)

        # Fully connected linear layer maps the 64-D hidden state to 2 output logits (Real, Fake).
        self.fc = nn.Linear(64, 2)

    def forward(self, x):
        _, (hidden, _) = self.lstm(x)
        # Selects the final hidden state [-1] after the entire 30-frame sequence is processed,
        # passing it to the linear layer for the final classification computation.
        return self.fc(hidden[-1])

model = RNNClassifier().to(device)

#  the Adam optimizer updates network weights based on gradients.
optimizer = optim.Adam(model.parameters(), lr=0.001)

# use a 3.0  weight to the minority class (Real) loss calculation.
# compensates for the 1:3 dataset imbalance.
class_weights = torch.tensor([3.0, 1.0]).to(device)

# Instantiate the loss function to compute the divergence between predicted logits and true labels.
criterion = nn.CrossEntropyLoss(weight=class_weights)

EPOCHS = 20
print(f"Target Epochs: {EPOCHS}")

for epoch in range(EPOCHS):
    model.train() # Enable dropout and batch normalization for training.
    total_loss = 0
    correct = 0
    total = 0

    for inputs, labels in train_loader:
        inputs, labels = inputs.to(device), labels.to(device)

        # Clear residual gradients from the previous iteration.
        optimizer.zero_grad()

        # Execute forward pass to generate classification logits.
        outputs = model(inputs)

        # Calculate the weighted Cross-Entropy Loss.
        loss = criterion(outputs, labels)

        # Execute backward pass to compute gradients via backpropagation.
        loss.backward()

        # Update model parameters (weights and biases) using the optimizer.
        optimizer.step()

        total_loss += loss.item()

        # Extract the highest probability class index from the output logits.
        _, predicted = torch.max(outputs, 1)

        correct += (predicted == labels).sum().item()
        total += labels.size(0)

    acc = 100 * correct / total
    print(f"    Epoch {epoch+1:02d} | Loss: {total_loss/len(train_loader):.4f} | Accuracy: {acc:.2f}%")

# Serialize the trained LSTM state dictionary (weights/biases) to disk.
torch.save(model.state_dict(), "rnn_weights.pth")

print("MODEL FUSION & EXPORT")

# Defines a singular computational graph containing both the CNN and LSTM for deployment.
class UnifiedModel(nn.Module):
    def __init__(self):
        super().__init__()
        resnet = models.resnet18(pretrained=True)
        self.cnn = nn.Sequential(*list(resnet.children())[:-1])
        self.lstm = nn.LSTM(512, 64, batch_first=True)
        self.fc = nn.Linear(64, 2)

    def forward(self, x):
        # x shape: (Batch_Size, 30_Frames, 3_Channels, 224, Height, 224_Width)
        b, seq, c, h, w = x.size()

        # Collapse the batch and sequence dimensions to process all frames simultaneously through the CNN.
        x = x.view(b * seq, c, h, w)

        features = self.cnn(x)

        # Restore the temporal sequence dimension required for LSTM matrix multiplication.
        features = features.view(b, seq, -1)

        _, (hidden, _) = self.lstm(features)

        return self.fc(hidden[-1])

export_model = UnifiedModel()

# load the LSTM weights
rnn_state = torch.load("rnn_weights.pth")

# look for and inject the trained LSTM weights into the unified architecture shell.
export_model.lstm.load_state_dict({k.replace('lstm.', ''): v for k, v in rnn_state.items() if 'lstm' in k})
export_model.fc.load_state_dict({k.replace('fc.', ''): v for k, v in rnn_state.items() if 'fc' in k})

# Freeze the final unified model for inference.
export_model.eval()

#make a mock tensor matching the expected input shape to trace the computational graph.
dummy_input = torch.randn(1, 30, 3, 224, 224)
output_file = "/content/deepfake_model9.onnx"

# Serialize the traced graph to ONNX
#dynmaic axes allow variable batch sizes when testing in app.py
torch.onnx.export(
    export_model,
    dummy_input,
    output_file,
    input_names=['input'],
    output_names=['output'],
    dynamic_axes={'input': {0: 'batch_size'}, 'output': {0: 'batch_size'}}
)

print(f"model saved to: {output_file}")
from google.colab import files
files.download(output_file)
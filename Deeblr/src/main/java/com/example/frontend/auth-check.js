
(function() {
    const token = localStorage.getItem("jwt_token");

    if (!token) {
        // 1. No Token? Show Popup and Kick them out
        alert("Login to access this!");
        window.location.href = "login.html";
    } else {
        // 2. Token exists? Verify it's not fake by making a test call (Optional but Recommended)
        // If your API returns 401 later, your page logic should also catch that.
    }
})();
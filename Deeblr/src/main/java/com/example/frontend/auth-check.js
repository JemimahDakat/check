
(function() {
    const token = localStorage.getItem("jwt_token");

    if (!token) {
        //no Token? Show Popup and Kick them out
        alert("Login to access this!");
        window.location.href = "login.html";
    } else {
        //token exists? Verify it's not fake by making a test call
    }
})();
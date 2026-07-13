window.onload = function() {
    var widthPercent = 0.35;
    var heightPercent = 0.85;

    var width = Math.round(window.screen.availWidth * widthPercent);
    var height = Math.round(window.screen.availHeight * heightPercent);
    var left = window.screen.availWidth - width;
    var top = Math.round((window.screen.availHeight - height) / 2);

    window.resizeTo(width, height);
    window.moveTo(left, top);
};


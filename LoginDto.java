document.addEventListener('keydown', function(e) {
    var scrollBody = document.querySelector('.ui-datatable-scrollable-body');
    if (!scrollBody) return;
    
    if (e.key === 'ArrowDown') {
        scrollBody.scrollTop += 30;
    } else if (e.key === 'ArrowUp') {
        scrollBody.scrollTop -= 30;
    }
});

<h:head>
    <link id="favicon" rel="shortcut icon" href="data:image/x-icon;," type="image/x-icon" />
</h:head>
     
     onstart="document.getElementById('favicon').href = '#{resource['images/loading.gif']}';" 
                 oncomplete="document.getElementById('favicon').href = '#{resource['images/favicon.ico']}';" />

 function pollWindowClose(win) {
        var timer = setInterval(function() {
            if (win.closed) {
                clearInterval(timer);
                returnListenerCommand();
            }
        }, 500);
    }

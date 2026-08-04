document.addEventListener('keydown', function(e) {
    var scrollBody = document.querySelector('.ui-datatable-scrollable-body');
    if (!scrollBody) return;
    
    if (e.key === 'ArrowDown') {
        scrollBody.scrollTop += 30;
    } else if (e.key === 'ArrowUp') {
        scrollBody.scrollTop -= 30;
    }
});


 <link id="favicon" rel="shortcut icon" href="#{resource['images/favicon.ico']}" type="image/x-icon" />
       onstart="document.getElementById('favicon').href = '#{resource['images/loading.gif']}';" 
                 oncomplete="document.getElementById('favicon').href = '#{resource['images/favicon.ico']}';" />


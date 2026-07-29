document.addEventListener('keydown', function(e) {
    var scrollBody = document.querySelector('.ui-datatable-scrollable-body');
    if (!scrollBody) return;
    
    if (e.key === 'ArrowDown') {
        scrollBody.scrollTop += 30;
    } else if (e.key === 'ArrowUp') {
        scrollBody.scrollTop -= 30;
    }
});

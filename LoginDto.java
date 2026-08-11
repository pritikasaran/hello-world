document.addEventListener('keydown', function(e) {
    if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return;
    var table = document.activeElement.closest('.ui-datatable');
    if (!table) return;

    e.preventDefault();
    var rows = [...table.querySelectorAll('tbody tr')];
    var current = table.querySelector('tr.ui-state-highlight');
    var idx = rows.indexOf(current) + (e.key === 'ArrowDown' ? 1 : -1);

    if (rows[idx]) {
        rows[idx].click();
        rows[idx].focus();
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

    public void returnDialogDispatcher(ActionEvent event) {
    String actionName = FacesContext.getCurrentInstance()
            .getExternalContext().getRequestParameterMap().get("actionName");

    switch (actionName) {
        case "weiterleitung": returnWeiterleitenDialog(); break;
        case "andere":        returnAndereDialog(); break;
        case "dritt":         returnDrittDialog(); break;
        default: LOG.warn("Unknown return action: " + actionName);
    }
}
    }
<p:remoteCommand name="returnListenerCommand"
    actionListener="#{aktenansichtVC.returnDialogDispatcher}" />


    public void returnDialogDispatcher(ActionEvent event) {
    Map<String, String> params = FacesContext.getCurrentInstance()
            .getExternalContext().getRequestParameterMap();

    String actionName = params.get("actionName");
    String returnValue = params.get("returnValue");

    switch (actionName) {
        case "weiterleitung": returnWeiterleitenDialog(returnValue); break;
        case "andere":        returnAndereDialog(returnValue); break;
        default: LOG.warn("Unknown return action: " + actionName);
    }
}


var lastReturnValue = null;

window.addEventListener('message', function(event) {
    if (event.data && event.data.value !== undefined) {
        lastReturnValue = event.data.value;
    }
});

function pollWindowClose(win, actionName) {
    var timer = setInterval(function() {
        if (win.closed) {
            clearInterval(timer);
            returnListenerCommand([
                { name: 'actionName', value: actionName },
                { name: 'returnValue', value: lastReturnValue || '' }
            ]);
            lastReturnValue = null; // reset for next use
        }
    }, 500);
}
private String escapeJs(String value) {
    if (value == null) return "";
    return value.replace("\\", "\\\\").replace("'", "\\'");
}

function closeAndReturn(value) {
    if (window.opener) {
        window.opener.postMessage({ value: value }, '*');
    }
    window.close();
}




    

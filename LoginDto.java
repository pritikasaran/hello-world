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


    

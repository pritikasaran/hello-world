function forceViewOneRedraw() {
    var obj = document.querySelector('object[class="com.ibm.dv.client.Viewer"]');
    if (!obj) return;
    obj.style.display = 'none';
    void obj.offsetHeight; // force reflow
    obj.style.display = '';
}

function scrollRowIntoTableView(rowElement) {
    var scrollBody = rowElement.closest('.ui-datatable-scrollable-body');
    if (!scrollBody) return;

    var rowTop = rowElement.offsetTop;
    var rowHeight = rowElement.offsetHeight;
    var containerHeight = scrollBody.clientHeight;

    // center the row within the scrollable container
    scrollBody.scrollTop = rowTop - (containerHeight / 2) + (rowHeight / 2);
}

const element = document.getElementById('myElement');

element.addEventListener('click', function(e) {
  if (e.ctrlKey) {
    console.log('Control + Click detected!');
    // Insert your Control+Click specific logic here
  } else {
    console.log('Normal Click detected.');
  }
});
const element = document.getElementById('myElement');

element.addEventListener('dblclick', function(e) {
  if (e.ctrlKey || e.metaKey) {
    console.log('Control / Command + Double-Click detected!');
    e.preventDefault(); // Optional: stops default browser text selection
  }
});

function syncValuesToHtml(container) {
    container.querySelectorAll('input, textarea, select').forEach(function(el) {
        if (el.tagName === 'SELECT') {
            [...el.options].forEach(o => o.selected ? o.setAttribute('selected','selected') : o.removeAttribute('selected'));
        } else if (el.type === 'checkbox' || el.type === 'radio') {
            el.checked ? el.setAttribute('checked','checked') : el.removeAttribute('checked');
        } else if (el.tagName === 'TEXTAREA') {
            el.textContent = el.value;
        } else {
            el.setAttribute('value', el.value);
        }
    });
}

document.addEventListener('click', function(e) {
    var t = e.target.closest('.ui-datatable');
    if (t) activeTable = t;
});

document.addEventListener('keydown', function(e) {
    if (!activeTable || (e.key !== 'ArrowDown' && e.key !== 'ArrowUp')) return;
    e.preventDefault();

    var widget = PF(activeTable.id.split(':').pop() + 'Widget'); // adjust if widgetVar naming differs
    var rows = [...activeTable.querySelectorAll('tbody tr')];
    var idx = rows.indexOf(activeTable.querySelector('tr.ui-state-highlight')) + (e.key === 'ArrowDown' ? 1 : -1);

    if (rows[idx]) widget.selectRow(idx, e);
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




    

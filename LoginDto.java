var lastClickTime = 0;
var lastClickedRow = null;
var DBLCLICK_THRESHOLD = 400; // ms

document.addEventListener('click', function(e) {
    var table = e.target.closest('.ui-datatable');
    if (!table) return;

    var row = e.target.closest('tbody tr');
    if (!row) return;

    var now = Date.now();
    var isDblClick = (row === lastClickedRow) && ((now - lastClickTime) < DBLCLICK_THRESHOLD);

    if (isDblClick) {
        dblClickHandler(row, table);
        lastClickTime = 0;
        lastClickedRow = null;
    } else {
        lastClickTime = now;
        lastClickedRow = row;
    }
});

document.addEventListener('dblclick', function(e) {
    var table = e.target.closest('.ui-datatable');
    if (!table) return;

    var row = e.target.closest('tbody tr');
    if (!row) return;

    dblClickHandler(row, table);
});

function dblClickHandler(row, table) {
    var element = document.getElementById('mainForm:cmdTerminBearbeiten');
    // adjust the form/naming-container prefix to match your actual rendered id
    if (element != null) {
        element.click();
    }
}

<p:remoteCommand name="handleRowDblClick"
                  actionListener="#{bean.onRowDoubleClick}"
                  update="@none" />


    private String getParam(String name) {
    return FacesContext.getCurrentInstance()
            .getExternalContext()
            .getRequestParameterMap()
            .get(name);
}

public void onRowDoubleClick() {
    String rowKey = getParam("rowKey");
    String sourceTable = getParam("sourceTable");

    List<AkteEintrag> targetList = sourceTable.endsWith("tabA1")
            ? filteredEList
            : allEList;

    AkteEintrag entry = findByGuid(targetList, rowKey);
    if (entry != null) {
        // your double-click logic
    }
}

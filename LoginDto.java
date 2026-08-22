document.addEventListener('dblclick', function(e) {
    var table = e.target.closest('.ui-datatable');
    if (!table) return;

    var row = e.target.closest('tr[data-rk]');
    if (!row) return;

    var widgetVar = table.id.split(':').pop() + 'Widget'; // adjust if your widgetVar naming differs
    var widget = PF(widgetVar);
    if (!widget) return;

    var rowKey = row.getAttribute('data-rk');

    // route to a generic remoteCommand, passing which table it came from
    handleRowDblClick([
        { name: 'rowKey', value: rowKey },
        { name: 'sourceTable', value: table.id }
    ]);
});

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

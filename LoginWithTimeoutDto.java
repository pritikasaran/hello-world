document.getElementById('panelTabbs:akSubForm:tabAlleEintraege').addEventListener('click', function(e) {
    if (e.detail === 2) return; // skip on the 2nd click of a dblclick, let rowDblselect handle it
    var row = e.target.closest('tr');
    if (!row) return;
    var idx = [...row.parentNode.children].indexOf(row);
    onRowClick([{name: 'idx', value: idx}, {name: 'ctrl', value: e.ctrlKey}]);
});
<p:remoteCommand name="onRowClick" actionListener="#{akteneintraegeVC.onRowClick}" update="tabAlleEintraege" />

public void onRowClick(ActionEvent event) {
    LOG.info("Start onRowClick");

    Map<String, String> params = FacesContext.getCurrentInstance()
        .getExternalContext().getRequestParameterMap();
    int idx = Integer.parseInt(params.get("idx"));
    boolean ctrl = Boolean.parseBoolean(params.get("ctrl"));

    List<Eintrag> alle = akteneintraegeModell.getAktenEintraege();
    if (alle == null || idx < 0 || idx >= alle.size()) {
        LOG.info("onRowClick - invalid idx: " + idx);
        return;
    }
    Eintrag selected = alle.get(idx);

    List<Eintrag> selList = akteneintraegeModell.getSelAktenEintraege();
    if (selList == null) {
        selList = new ArrayList<>();
    }

    if (ctrl) {
        if (selList.contains(selected)) {
            selList.remove(selected);
        } else {
            selList.add(selected);
        }
    } else {
        selList.clear();
        selList.add(selected);
    }

    akteneintraegeModell.setSelAktenEintraege(selList);
    akteModell.setEintraege(selList);

    LOG.info("onRowClick - Anzahl selList: " + selList.size());
    LOG.info("End onRowClick");
}

public boolean isSelected(Eintrag e) {
    List<Eintrag> sel = akteneintraegeModell.getSelAktenEintraege();
    return sel != null && sel.contains(e);
}

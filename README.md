if (result.startsWith("error")) {
    // replace JsfUtil.showError with this
    FacesContext.getCurrentInstance().addMessage(null,
        new FacesMessage(FacesMessage.SEVERITY_ERROR, 
            "Fehler", result));
    return null;
}

<p:growl id="msgs" 
         showDetail="true" 
         showSummary="true"
         life="5000"/>

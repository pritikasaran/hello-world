public String getUrl() {
        return "/IMS-Frontend/faces/pages/" + page + ".xhtml";
    }
    
    // add window open script
    public String getWindowOpenScript() {
        return "window.open('" + getUrl() + "', '_blank', 'width=600,height=400,resizable=yes')";
    }

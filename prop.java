  writeLine((urlParamsStarted ? "&" : "?") 
            + name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8), writer);
        urlParamsStarted = true;



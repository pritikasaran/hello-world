<p:menubar>
    <!-- Postkorb Menu -->
    <p:submenu label="Postkorb">
        <p:menuitem value="Neu Laden"
            action="#{postkorbVC.actionAktualisieren}" ajax="true"
            target="_blank">
            <h:graphicImage library="resources/images" name="refresh.gif" alt="Neu Laden"/>
        </p:menuitem>

        <p:menuitem value="Sortieren"
            action="#{postkorbVC.sortierungEinstellen}" ajax="true"
            title="Postkorb sortieren" target="_blank">
            <h:graphicImage library="resources/images" name="sortieren.gif" alt="Sortieren"/>
        </p:menuitem>

        <p:menuitem value="Öffnen/Schließen"
            action="#{postkorbVC.openClose}" ajax="true" target="_blank">
            <p:setPropertyActionListener value="#{postkorbVC.postkorbModell.postkorbID}"
                target="#{postkorbOpenCloseVC.modell.postkorbID}" />
            <p:setPropertyActionListener value="true"
                target="#{postkorbOpenCloseVC.modell.resetModell}" />
        </p:menuitem>

        <p:menuitem value="Berechtigung"
            action="#{postkorbVC.berechtigung}" ajax="true" target="_blank"
            rendered="#{postkorbVC.imsModell.benutzer.orgPosition eq 'A' or postkorbVC.imsModell.benutzer.orgPosition eq 'G'}">
            <p:setPropertyActionListener value="#{postkorbVC.postkorbModell.postkorbID}"
                target="#{postkorbBerechtVC.modell.postkorbID}" />
            <p:setPropertyActionListener value="true"
                target="#{postkorbBerechtVC.modell.resetModell}" />
        </p:menuitem>
    </p:submenu>

    <!-- Eintrag Menu -->
    <p:submenu label="Eintrag">
        <p:menuitem value="Termin bearbeiten"
            action="#{postkorbVC.bearbeiteTermin}" target="_blank">
            <h:graphicImage library="resources/images" name="bearbeiten.gif" alt="Bearbeiten"/>
        </p:menuitem>

        <p:menuitem value="Weiterleiten/Wiedervorlage"
            action="#{postkorbVC.weiterleiten}" ajax="true" target="_blank"
            rendered="#{!postkorbVC.postkorbModell.filter.erledigteTermine}">
            <h:graphicImage library="resources/images" name="weiterleiten.gif" alt="Weiterleiten"/>
        </p:menuitem>

        <p:menuitem value="Kennzeichen ändern"
            action="#{postkorbVC.kennzeichenAendern}" ajax="true" target="_blank"
            rendered="#{!postkorbVC.postkorbModell.filter.erledigteTermine}">
            <h:graphicImage library="resources/images" name="kzaendern2.gif" alt="Kennzeichen ändern"/>
        </p:menuitem>

        <p:menuitem value="Erledigen"
            action="#{postkorbVC.erledigenTermin}" ajax="true" target="_blank"
            rendered="#{!postkorbVC.postkorbModell.filter.erledigteTermine}">
            <h:graphicImage library="resources/images" name="termin_erled.gif" alt="Erledigen"/>
        </p:menuitem>
    </p:submenu>

    <!-- Hilfe Menu -->
    <p:submenu label="Hilfe">
        <p:menuitem value="Anzeige Handbuch IMS"
            action="#{postkorbVC.openIMSHandbuch}" ajax="true" target="_blank">
            <h:graphicImage library="resources/images" name="information.gif" alt="IMS"/>
        </p:menuitem>

        <p:menuitem value="Anzeige Handbuch KKE"
            action="#{postkorbVC.openKKEHandbuch}" ajax="true" target="_blank">
            <h:graphicImage library="resources/images" name="information.gif" alt="KKE"/>
        </p:menuitem>

        <p:menuitem value="Anzeige Handbuch BQM"
            action="#{postkorbVC.openBQMHandbuch}" ajax="true" target="_blank">
            <h:graphicImage library="resources/images" name="information.gif" alt="BQM"/>
        </p:menuitem>

        <p:menuitem value="Info"
            action="#{postkorbVC.showInfo}" ajax="true" target="_blank">
            <h:graphicImage library="resources/images" name="information.gif" alt="Info"/>
        </p:menuitem>

        <p:menuitem value="Teste Fehlerseite"
            action="#{postkorbVC.showTestError}" ajax="true" target="_blank"
            rendered="#{postkorbVC.showTestButtons}">
            <h:graphicImage library="resources/images" name="information.gif" alt="Test"/>
        </p:menuitem>
    </p:submenu>
</p:menubar>

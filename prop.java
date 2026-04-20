<p:dataTable id="tabPostkorb"
             value="#{postkorbVC.postkorbModell.eintraege}"
             var="eintrag"
             selection="#{postkorbVC.postkorbModell.selectedEintraege}"
             selectionMode="multiple"
             emptyMessage="#{postkorbVC.postkorbModell.emptyTextTable}"
             style="width:100%; height:#{postkorbVC.postkorbModell.heightTable}px;"
             rowKey="#{eintrag.id.postkorbId}"
             paginator="true"
             rows="#{postkorbVC.fetchSizeTable}"
             resizableColumns="false">

    <!-- Double click event -->
    <p:ajax event="rowDblselect" listener="#{postkorbVC.dblClickPostkorb}" update="@form" />

    <!-- Column: IKz -->
    <p:column headerText="IKz" style="width:55px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.terminIndivText}" title="#{eintrag.id.terminLnr}" />
    </p:column>

    <!-- Column: Art -->
    <p:column headerText="Art" style="width:42px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.terminArt}" 
                      title="#{eintrag.terminArt}: #{eintrag.terminArtBez}" />
    </p:column>

    <!-- Column: Eing-Dat -->
    <p:column headerText="Eing-Dat" style="width:105px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.eingangDat}">
            <f:convertDateTime pattern="dd.MM.yyyy" />
        </h:outputText>
    </p:column>

    <!-- Column: GRD -->
    <p:column headerText="GRD" style="width:55px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.terminGrund}" title="#{eintrag.terminGrundBez}" />
    </p:column>

    <!-- Column: MDM -->
    <p:column headerText="MDM" style="width:45px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.medium}" title="#{eintrag.mediumBez}" />
    </p:column>

    <!-- Column: Dk -->
    <p:column headerText="Dk" style="width:30px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.dokukl}" />
    </p:column>

    <!-- Column: Termin-Dat -->
    <p:column headerText="Termin-Dat" style="width:105px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.terminDat}">
            <f:convertDateTime pattern="dd.MM.yyyy" />
        </h:outputText>
    </p:column>

    <!-- Column: VG-Art -->
    <p:column headerText="VG-Art" style="width:120px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.vgartKurzBez}" 
                      title="#{eintrag.vgart}: #{eintrag.vgartLangBez}" />
    </p:column>

    <!-- Column: Textkurzverweis -->
    <p:column headerText="Textkurzverweis" style="width:150px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.textkvKurzBez}" 
                      title="#{eintrag.textkv}: #{eintrag.textkvLangBez}" />
    </p:column>

    <!-- Column: A/E -->
    <p:column headerText="A/E" style="width:48px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.anlassgeber}" />
    </p:column>

    <!-- Column: ORB -->
    <p:column headerText="ORB" style="width:125px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.orb1}" />
    </p:column>

    <!-- Column: OrbA -->
    <p:column headerText="OrbA" style="width:65px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.orbartKurzBez}" 
                      title="#{eintrag.orbart}: #{eintrag.orbartLangBez}" />
    </p:column>

    <!-- Column: MA -->
    <p:column headerText="MA" style="width:42px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.mandantKurzBez}" 
                      title="#{eintrag.mandant}: #{eintrag.mandantLangBez}" />
    </p:column>

    <!-- Column: RD -->
    <p:column headerText="RD" style="width:30px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.rd}" />
    </p:column>

    <!-- Column: VW -->
    <p:column headerText="VW" style="width:36px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.vertWeg}" />
    </p:column>

    <!-- Column: Bearbeiter -->
    <p:column headerText="Bearbeiter" style="width:100px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.absender}" />
    </p:column>

    <!-- Column: Postkorb ID -->
    <p:column headerText="Postkorb ID" style="width:115px;" styleClass="imsTableHeader">
        <h:outputText value="#{eintrag.id.postkorbId}" />
    </p:column>

</p:dataTable>

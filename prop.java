<af:table id="tabPostkorb" inlineStyle="height:#{postkorbVC.postkorbModell.heightTable}px;"
				value="#{postkorbVC.postkorbModell.eintraege}" var="eintrag"
				rowSelection="multiple" rowBandingInterval="0"
				emptyText="#{postkorbVC.postkorbModell.emptyTextTable}"
				selectedRowKeys="#{postkorbVC.postkorbModell.pkRowKeySet}"
				contentDelivery="immediate" displayRow="selected"
				binding="#{postkorbVC.postkorbTable}"
				styleClass="AFStretchWidth"
				fetchSize="#{postkorbVC.fetchSizeTable}"
				disableColumnReordering="true">

				<af:clientListener type="dblClick" method="dblClickPostkorb" />
				<af:clientListener type="load" method="rewriteAdfSetFocusOnEditableElementInNode" />
				<af:column headerText="IKz" shortDesc="Individuelles Kennzeichen"
					sortProperty="terminIndivText" sortable="false" noWrap="true"
					id="IKz" width="55px" headerClass="imsTableHeader">
					<af:outputText value="#{eintrag.terminIndivText}"
						shortDesc="#{eintrag.id.terminLnr}" />
				</af:column>
				<af:column headerText="Art" shortDesc="Art des Termins"
					sortProtperty="terminArt" sortable="false" noWrap="true"
					width="42px" styleClass="imsTableData" headerClass="imsTableHeader">
					<af:outputText value="#{eintrag.terminArt}"
						shortDesc="#{eintrag.terminArt}: #{eintrag.terminArtBez}">
					</af:outputText>
				</af:column>
				<af:column headerText="Eing-Dat" headerNoWrap="true"
					shortDesc="Eingangsdatum des Dokuments" sortProperty="eingangDat"
					sortable="false" noWrap="true" id="eingangDat" width="105px"
					headerClass="imsTableHeader">
					<af:outputFormatted converter="DatumKurz"
						value="#{eintrag.eingangDat}" />
				</af:column>
				<af:column headerText="GRD" shortDesc="Grund des Termins"
					sortProperty="terminGrund" sortable="false" noWrap="true"
					width="55px" headerClass="imsTableHeader">
					<af:outputText value="#{eintrag.terminGrund}"
						shortDesc="#{eintrag.terminGrundBez}" />
				</af:column>
				<af:column headerText="MDM" shortDesc="Medium"
					sortProperty="medium" sortable="false" noWrap="true"
					width="45px" headerClass="imsTableHeader">
					<af:outputText value="#{eintrag.medium}"
						shortDesc="#{eintrag.mediumBez}" />
				</af:column>
				<af:column headerText="Dk" shortDesc="Dokumentenklasse"
					sortProperty="dokukl" sortable="false" noWrap="true" id="dokukl"
					width="30px" headerClass="imsTableHeader">
					<af:outputText value="#{eintrag.dokukl}"
						shortDesc="#{eintrag.dokukl}" />
				</af:column>
				<af:column headerText="Termin-Dat" headerNoWrap="true"
					shortDesc="Datum des Termins" sortProperty="terminDat"
					sortable="false" noWrap="true" id="terminDat" width="105px"
					headerClass="imsTableHeader">
					<af:outputFormatted converter="DatumKurz"
						value="#{eintrag.terminDat}" />
				</af:column>
				<af:column headerText="VG-Art" headerNoWrap="true"
					shortDesc="Vorgangsart" sortProperty="vgartKurzBez"
					sortable="false" noWrap="true" id="vgartKurzBez" width="120px"
					headerClass="imsTableHeader">
					<af:outputText value="#{eintrag.vgartKurzBez}"
						shortDesc="#{eintrag.vgart}: #{eintrag.vgartLangBez}" />
				</af:column>
				<af:column headerText="Textkurzverweis" shortDesc="Textkurzverweis"
					sortProperty="textkvKurzBez" sortable="false" noWrap="true"
					id="textkvKurzBez" width="150px" headerClass="imsTableHeader"
					styleClass="imsTableData">
					<af:outputText value="#{eintrag.textkvKurzBez}"
						shortDesc="#{eintrag.textkv}: #{eintrag.textkvLangBez}" />
				</af:column>
				<af:column headerText="A/E" shortDesc="Anlassgeber/Empfänger"
					sortProperty="anlassgeber" sortable="false" noWrap="true"
					id="anlassgeber" width="48px" headerClass="imsTableHeader"
					styleClass="imsTableData">
					<af:outputText value="#{eintrag.anlassgeber}"
						shortDesc="#{eintrag.anlassgeber}" />
				</af:column>
				<af:column headerText="ORB" shortDesc="Ordnungsbegriff"
					sortProperty="orb1" sortable="false" noWrap="true" id="orb1"
					width="125px" headerClass="imsTableHeader">
					<af:outputText value="#{eintrag.orb1}" shortDesc="#{eintrag.orb1}" />
				</af:column>
				<af:column headerText="OrbA" shortDesc="Art des Ordnungsbegriffs"
					sortProperty="orbartKurzBez" sortable="false" noWrap="true"
					id="orbartKurzBez" width="65px" headerClass="imsTableHeader"
					styleClass="imsTableData">
					<af:outputText value="#{eintrag.orbartKurzBez}"
						shortDesc="#{eintrag.orbart}: #{eintrag.orbartLangBez}" />
				</af:column>
				<af:column headerText="MA" shortDesc="Mandant"
					sortProperty="mandantKurzBez" sortable="false" noWrap="true"
					id="mandantKurzBez" width="42px" headerClass="imsTableHeader"
					styleClass="imsTableData">
					<af:outputText value="#{eintrag.mandantKurzBez}"
						shortDesc="#{eintrag.mandant}: #{eintrag.mandantLangBez}" />
				</af:column>
				<af:column headerText="RD" shortDesc="Regionaldirektion"
					sortProperty="rd" sortable="false" noWrap="true" id="rd"
					width="30px" headerClass="imsTableHeader">
					<af:outputText value="#{eintrag.rd}" />
				</af:column>
				<af:column headerText="VW" shortDesc="Vertriebsweg"
					sortProperty="vertWeg" sortable="false" noWrap="true" id="vertWeg"
					width="36px" headerClass="imsTableHeader">
					<af:outputText value="#{eintrag.vertWeg}" />
				</af:column>
				<af:column headerText="Bearbeiter" shortDesc="Bearbeiter"
					sortProperty="absender" sortable="false" noWrap="true"
					id="absender" width="100px" headerClass="imsTableHeader">
					<af:outputText value="#{eintrag.absender}" />
				</af:column>
				<af:column headerText="Postkorb ID" headerNoWrap="true"
					shortDesc="Kennung des Postkorb-Inhabers"
					sortProperty="id.postkorbId" sortable="false" noWrap="true"
					id="pkid" width="115px" headerClass="imsTableHeader">
					<af:outputText value="#{eintrag.id.postkorbId}" />
				</af:column>
			</af:table>

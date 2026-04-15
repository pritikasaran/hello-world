		<af:menuBar>
			<af:menu text="Postkorb">
				<af:commandMenuItem text="Neu Laden"
					icon="/resources/images/refresh.gif"
					action="#{postkorbVC.actionAktualisieren}" partialSubmit="true">
				</af:commandMenuItem>
				<af:commandMenuItem text="Sortieren"
					icon="/resources/images/sortieren.gif"
					shortDesc="Postkorb sortieren" windowWidth="500" windowHeight="400"
					windowModalityType="applicationModal" useWindow="true"
					partialSubmit="true" action="#{postkorbVC.sortierungEinstellen}"
					returnListener="#{postkorbVC.returnFromSortierenDialog}" />
				<af:commandMenuItem text="Öffnen/Schließen" icon=""
					windowWidth="750" windowHeight="630"
					windowModalityType="applicationModal" useWindow="true"
					partialSubmit="true" action="#{postkorbVC.openClose}">
					<af:setActionListener
						from="#{postkorbVC.postkorbModell.postkorbID}"
						to="#{postkorbOpenCloseVC.modell.postkorbID}" />
					<af:setActionListener from="#{true}"
						to="#{postkorbOpenCloseVC.modell.resetModell}" />
				</af:commandMenuItem>

				<af:commandMenuItem text="Berechtigung" icon=""
					rendered="#{(postkorbVC.imsModell.benutzer.orgPosition eq 'A') or (postkorbVC.imsModell.benutzer.orgPosition eq 'G')}"
					windowWidth="750" windowHeight="750"
					windowModalityType="applicationModal" useWindow="true"
					action="#{postkorbVC.berechtigung}">
					<af:setActionListener
						from="#{postkorbVC.postkorbModell.postkorbID}"
						to="#{postkorbBerechtVC.modell.postkorbID}" />
					<af:setActionListener from="#{true}"
						to="#{postkorbBerechtVC.modell.resetModell}" />
				</af:commandMenuItem>
			</af:menu>
			<af:menu text="Eintrag">
				<af:commandMenuItem text="Termin bearbeiten"
					icon="/resources/images/bearbeiten.gif"
					binding="#{postkorbVC.cmdMenuItemTerminBearbeiten}"
					action="#{postkorbVC.bearbeiteTermin}">
				</af:commandMenuItem>
				<af:commandMenuItem text="Weiterleiten/Wiedervorlage"
					rendered="#{!postkorbVC.postkorbModell.filter.erledigteTermine}"
					useWindow="true" windowWidth="510" windowHeight="700"
					windowModalityType="applicationModal"
					action="#{postkorbVC.weiterleiten}" partialSubmit="true"
					returnListener="#{postkorbVC.returnWeiterleitenDialog}"
					icon="/resources/images/weiterleiten.gif" />
				<af:commandMenuItem text="Kennzeichen ändern"
					rendered="#{!postkorbVC.postkorbModell.filter.erledigteTermine}"
					icon="/resources/images/kzaendern2.gif"
					action="#{postkorbVC.kennzeichenAendern}" partialSubmit="true" />
				<af:commandMenuItem text="Erledigen" partialSubmit="true"
					rendered="#{!postkorbVC.postkorbModell.filter.erledigteTermine}"
					windowModalityType="applicationModal" useWindow="true"
					action="#{postkorbVC.erledigenTermin}"
					icon="/resources/images/termin_erled.gif" />
			</af:menu>
			<af:menu text="Hilfe">
				<af:commandMenuItem text="Anzeige Handbuch IMS"
					windowModalityType="applicationModal" useWindow="true"
                    windowWidth="750" windowHeight="900" partialSubmit="true"
					action="#{postkorbVC.openIMSHandbuch}"
					icon="/resources/images/information.gif" />
				<af:commandMenuItem text="Anzeige Handbuch KKE"
					windowModalityType="applicationModal" useWindow="true"
                    windowWidth="750" windowHeight="900" partialSubmit="true"
					action="#{postkorbVC.openKKEHandbuch}"
					icon="/resources/images/information.gif" />
				<af:commandMenuItem text="Anzeige Handbuch BQM"
					windowModalityType="applicationModal" useWindow="true"
                    windowWidth="750" windowHeight="900" partialSubmit="true"
					action="#{postkorbVC.openBQMHandbuch}"
					icon="/resources/images/information.gif" />
				<af:commandMenuItem text="Info"
					windowModalityType="applicationModal" useWindow="true"
					windowWidth="200" windowHeight="200" partialSubmit="true"
					action="#{postkorbVC.showInfo}"/>
				<af:commandMenuItem text="Teste Fehlerseite"
					windowModalityType="applicationModal" useWindow="true"
					windowWidth="750" windowHeight="900"
					action="#{postkorbVC.showTestError}"
					rendered="#{postkorbVC.showTestButtons}" />
			</af:menu>
		</af:menuBar>

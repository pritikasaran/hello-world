package de.deltalloyd.partnerdb.webservices.impl;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.deltalloyd.foundation.auth.exception.AuthorisationException;
import de.deltalloyd.foundation.exception.AbstractCheckedSystemException;
import de.deltalloyd.partnerdb.core.monitor.appreg.MyAppPerformanceSensor;
import de.deltalloyd.partnerdb.core.type.NatuerlichePersonGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.NatuerlichePersonTO;
import de.deltalloyd.partnerdb.core.type.NichtNatuerlichePersonGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.NichtNatuerlichePersonTO;
import de.deltalloyd.partnerdb.core.type.PartnerGemeinschaftGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.PartnerGemeinschaftTO;
import de.deltalloyd.partnerdb.orb.impl.OrbServiceEJB;
import de.deltalloyd.partnerdb.orb.service.type.FunktionServiceTO;
import de.deltalloyd.partnerdb.orb.service.type.OrdnungsbegriffServiceTO;
import de.deltalloyd.partnerdb.orb.type.FunktionTO;
import de.deltalloyd.partnerdb.orb.type.OrdnungsbegriffSuchTO;
import de.deltalloyd.partnerdb.orb.type.OrdnungsbegriffTO;
import de.deltalloyd.partnerdb.security.profiles.PdbBerechtigungen;
import de.deltalloyd.partnerdb.webservices.exception.PDBException;
import de.deltalloyd.partnerdb.webservices.exception.PartnerWebServiceException;
import de.deltalloyd.partnerdb.webservices.security.exception.STSException;
import de.deltalloyd.partnerdb.webservices.utils.PDBReleaseInfo;

@Stateless
@Interceptors(MyAppPerformanceSensor.class)
public class OrbService extends AbstractWebService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrbService.class);

    @EJB
    private OrbServiceEJB orbServiceLocal;

    public OrdnungsbegriffTO sucheOrdnungsbegriffService(String authToken, String orbArtSchluessel,
            String funkArtSchluessel, Integer aMandant, String aParameter) throws PartnerWebServiceException {
        try {
            LOGGER.debug("sucheOrdnungsbegriffService:Aufruf> orbArtSchluessel: " + orbArtSchluessel
                    + ", funkArtSchluessel: " + funkArtSchluessel + ", aMandant:" + aMandant);
            getSSTSecurityToken().isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);

            OrdnungsbegriffSuchTO ordnungsbegriffSuchTO = new OrdnungsbegriffSuchTO();
            ordnungsbegriffSuchTO.setOrbartSchluessel(orbArtSchluessel);
            ordnungsbegriffSuchTO.setOrbMandant(aMandant);
            ordnungsbegriffSuchTO.setOrbSchluessel(funkArtSchluessel);

            OrdnungsbegriffTO ordnungsbegriffTO = orbServiceLocal.sucheOrdnungsbegriff(ordnungsbegriffSuchTO);

            if (ordnungsbegriffTO == null) {
                LOGGER.error("sucheOrdnungsbegriffService:" + orbArtSchluessel + "," + funkArtSchluessel + ","
                        + aMandant + ":ORB nicht gefunden");
                throw new PartnerWebServiceException(
                        new PDBException(PDBException.DATENFEHLER, "ORB wurde nicht gefunden.", null));
            }

            boolean bVIPBerechtigung = false;
            try {
                bVIPBerechtigung = getSSTSecurityToken().isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
            } catch (Exception e) {
            }

            if (bVIPBerechtigung == false) {
                final String VIPMITARBEITER = "1";
                for (FunktionTO funktionTO : ordnungsbegriffTO.getFunktionen()) {
                    String vipKZ = funktionTO.getPartnerTO().getGrunddaten().getVipKennzeichen();
                    if ((vipKZ != null) && (vipKZ.trim().equals(VIPMITARBEITER))) {
                        long partnerNr = funktionTO.getPartnerTO().getGrunddaten().getPartnerNr();
                        int klient = funktionTO.getPartnerTO().getGrunddaten().getKlient();
                        if (funktionTO.getPartnerTO() instanceof NatuerlichePersonTO) {
                            funktionTO.setPartnerTO(new NatuerlichePersonTO());
                            funktionTO.getPartnerTO().setPartnerTyp("1");
                            funktionTO.getPartnerTO().setGrunddaten(new NatuerlichePersonGrunddatenTO());
                            funktionTO.getPartnerTO().getGrunddaten().setPartnerNr(partnerNr);
                            funktionTO.getPartnerTO().getGrunddaten().setKlient(klient);
                            funktionTO.getPartnerTO().getGrunddaten().setVipKennzeichen(VIPMITARBEITER);
                        } else if (funktionTO.getPartnerTO() instanceof NichtNatuerlichePersonTO) {
                            funktionTO.setPartnerTO(new NichtNatuerlichePersonTO());
                            funktionTO.getPartnerTO().setPartnerTyp("2");
                            funktionTO.getPartnerTO().setGrunddaten(new NichtNatuerlichePersonGrunddatenTO());
                            funktionTO.getPartnerTO().getGrunddaten().setPartnerNr(partnerNr);
                            funktionTO.getPartnerTO().getGrunddaten().setKlient(klient);
                            funktionTO.getPartnerTO().getGrunddaten().setVipKennzeichen(VIPMITARBEITER);
                        } else if (funktionTO.getPartnerTO() instanceof PartnerGemeinschaftTO) {
                            funktionTO.setPartnerTO(new PartnerGemeinschaftTO());
                            funktionTO.getPartnerTO().setPartnerTyp("3");
                            funktionTO.getPartnerTO().setGrunddaten(new PartnerGemeinschaftGrunddatenTO());
                            funktionTO.getPartnerTO().getGrunddaten().setPartnerNr(partnerNr);
                            funktionTO.getPartnerTO().getGrunddaten().setKlient(klient);
                            funktionTO.getPartnerTO().getGrunddaten().setVipKennzeichen(VIPMITARBEITER);
                        }
                    }
                }
            }

            return ordnungsbegriffTO;

        } catch (STSException e) {
            LOGGER.error(e.getMessage(), e);
            throw new PartnerWebServiceException(new PDBException(PDBException.KEINELESERECHTE, e.getUuid()));
        } catch (AuthorisationException e) {
            LOGGER.error(e.getMessage());
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
        } catch (AbstractCheckedSystemException e) {
            LOGGER.error(e.getMessage());
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new PartnerWebServiceException(new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage(), null));
        }
    }

    public OrdnungsbegriffServiceTO sucheOrdnungsbegriffServiceFull(String authToken, String orbArtSchluessel,
            String funkArtSchluessel, Integer aMandant, String aParameter) throws PartnerWebServiceException {
        try {
            LOGGER.debug("sucheOrdnungsbegriffService:Aufruf> orbArtSchluessel: " + orbArtSchluessel
                    + ", funkArtSchluessel: " + funkArtSchluessel + ", aMandant:" + aMandant);
            getSSTSecurityToken().isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);

            OrdnungsbegriffSuchTO ordnungsbegriffSuchTO = new OrdnungsbegriffSuchTO();
            ordnungsbegriffSuchTO.setOrbartSchluessel(orbArtSchluessel);
            ordnungsbegriffSuchTO.setOrbMandant(aMandant);
            ordnungsbegriffSuchTO.setOrbSchluessel(funkArtSchluessel);

            OrdnungsbegriffServiceTO ordnungsbegriffTO = orbServiceLocal.sucheOrdnungsbegriffFull(ordnungsbegriffSuchTO);

            if (ordnungsbegriffTO == null) {
                LOGGER.error("sucheOrdnungsbegriffService:" + orbArtSchluessel + "," + funkArtSchluessel + ","
                        + aMandant + ":ORB nicht gefunden");
                throw new PartnerWebServiceException(
                        new PDBException(PDBException.DATENFEHLER, "ORB wurde nicht gefunden.", null));
            }

            boolean bVIPBerechtigung = false;
            try {
                bVIPBerechtigung = getSSTSecurityToken().isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
            } catch (Exception e) {
            }

            if (bVIPBerechtigung == false) {
                final String VIPMITARBEITER = "1";
                for (FunktionServiceTO funktionTO : ordnungsbegriffTO.getFunktionen()) {
                    String vipKZ = funktionTO.getPartnerTO().getGrunddaten().getVipKennzeichen();
                    if ((vipKZ != null) && (vipKZ.trim().equals(VIPMITARBEITER))) {
                        long partnerNr = funktionTO.getPartnerTO().getGrunddaten().getPartnerNr();
                        int klient = funktionTO.getPartnerTO().getGrunddaten().getKlient();
                        if (funktionTO.getPartnerTO() instanceof NatuerlichePersonTO) {
                            funktionTO.setPartnerTO(new NatuerlichePersonTO());
                            funktionTO.getPartnerTO().setPartnerTyp("1");
                            funktionTO.getPartnerTO().setGrunddaten(new NatuerlichePersonGrunddatenTO());
                            funktionTO.getPartnerTO().getGrunddaten().setPartnerNr(partnerNr);
                            funktionTO.getPartnerTO().getGrunddaten().setKlient(klient);
                            funktionTO.getPartnerTO().getGrunddaten().setVipKennzeichen(VIPMITARBEITER);
                        } else if (funktionTO.getPartnerTO() instanceof NichtNatuerlichePersonTO) {
                            funktionTO.setPartnerTO(new NichtNatuerlichePersonTO());
                            funktionTO.getPartnerTO().setPartnerTyp("2");
                            funktionTO.getPartnerTO().setGrunddaten(new NichtNatuerlichePersonGrunddatenTO());
                            funktionTO.getPartnerTO().getGrunddaten().setPartnerNr(partnerNr);
                            funktionTO.getPartnerTO().getGrunddaten().setKlient(klient);
                            funktionTO.getPartnerTO().getGrunddaten().setVipKennzeichen(VIPMITARBEITER);
                        } else if (funktionTO.getPartnerTO() instanceof PartnerGemeinschaftTO) {
                            funktionTO.setPartnerTO(new PartnerGemeinschaftTO());
                            funktionTO.getPartnerTO().setPartnerTyp("3");
                            funktionTO.getPartnerTO().setGrunddaten(new PartnerGemeinschaftGrunddatenTO());
                            funktionTO.getPartnerTO().getGrunddaten().setPartnerNr(partnerNr);
                            funktionTO.getPartnerTO().getGrunddaten().setKlient(klient);
                            funktionTO.getPartnerTO().getGrunddaten().setVipKennzeichen(VIPMITARBEITER);
                        }
                    }
                }
            }

            return ordnungsbegriffTO;

        } catch (STSException e) {
            LOGGER.error(e.getMessage(), e);
            throw new PartnerWebServiceException(new PDBException(PDBException.KEINELESERECHTE, e.getUuid()));
        } catch (AuthorisationException e) {
            LOGGER.error(e.getMessage());
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
        } catch (AbstractCheckedSystemException e) {
            LOGGER.error(e.getMessage());
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new PartnerWebServiceException(new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage(), null));
        }
    }

    public String getReleaseInfo() {
        return PDBReleaseInfo.getReleaseInfo();
    }
}

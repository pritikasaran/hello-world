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
import de.deltalloyd.partnerdb.orb.type.FunktionTO;
import de.deltalloyd.partnerdb.orb.type.OrdnungsbegriffSuchTO;
import de.deltalloyd.partnerdb.orb.type.OrdnungsbegriffTO;
import de.deltalloyd.partnerdb.orb.service.type.OrdnungsbegriffServiceTO;
import de.deltalloyd.partnerdb.security.profiles.PdbBerechtigungen;
import de.deltalloyd.partnerdb.webservices.exception.PDBException;
import de.deltalloyd.partnerdb.webservices.exception.PartnerWebServiceException;
import de.deltalloyd.partnerdb.webservices.security.exception.STSException;
import de.deltalloyd.partnerdb.webservices.utils.PDBReleaseInfo;

@Stateless
@Interceptors(MyAppPerformanceSensor.class)
public class OrbService extends AbstractWebService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrbService.class);
    private static final String VIPMITARBEITER = "1";

    @EJB
    private OrbServiceEJB orbServiceLocal;

    public OrdnungsbegriffTO sucheOrdnungsbegriff(String authToken, String orbArtSchluessel,
            String funkArtSchluessel, Integer aMandant, String aParameter) throws PartnerWebServiceException {
        try {
            LOGGER.debug("sucheOrdnungsbegriffService:Aufruf> orbArtSchluessel: {}, funkArtSchluessel: {}, aMandant: {}",
                    orbArtSchluessel, funkArtSchluessel, aMandant);

            getSSTSecurityToken().isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);

            OrdnungsbegriffSuchTO suchTO = buildSuchTO(orbArtSchluessel, funkArtSchluessel, aMandant);
            OrdnungsbegriffTO result = orbServiceLocal.sucheOrdnungsbegriff(suchTO);

            if (result == null) {
                LOGGER.error("sucheOrdnungsbegriffService: {},{},{} :ORB nicht gefunden",
                        orbArtSchluessel, funkArtSchluessel, aMandant);
                throw new PartnerWebServiceException(
                        new PDBException(PDBException.DATENFEHLER, "ORB wurde nicht gefunden.", null));
            }

            if (!hasVipBerechtigung(authToken)) {
                maskVipPartner(result);
            }

            return result;

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
        } catch (PartnerWebServiceException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new PartnerWebServiceException(new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage(), null));
        }
    }

    public OrdnungsbegriffServiceTO sucheOrdnungsbegriffFull(String authToken, String orbArtSchluessel,
            String funkArtSchluessel, Integer aMandant, String aParameter) throws PartnerWebServiceException {
        try {
            LOGGER.debug("sucheOrdnungsbegriffServiceFull:Aufruf> orbArtSchluessel: {}, funkArtSchluessel: {}, aMandant: {}",
                    orbArtSchluessel, funkArtSchluessel, aMandant);

            getSSTSecurityToken().isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);

            OrdnungsbegriffSuchTO suchTO = buildSuchTO(orbArtSchluessel, funkArtSchluessel, aMandant);
            OrdnungsbegriffServiceTO result = orbServiceLocal.sucheOrdnungsbegriffFull(suchTO);

            if (result == null) {
                LOGGER.error("sucheOrdnungsbegriffServiceFull: {},{},{} :ORB nicht gefunden",
                        orbArtSchluessel, funkArtSchluessel, aMandant);
                throw new PartnerWebServiceException(
                        new PDBException(PDBException.DATENFEHLER, "ORB wurde nicht gefunden.", null));
            }

            if (!hasVipBerechtigung(authToken)) {
                maskVipPartnerFull(result);
            }

            return result;

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
        } catch (PartnerWebServiceException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new PartnerWebServiceException(new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage(), null));
        }
    }

    public String getReleaseInfo() {
        return PDBReleaseInfo.getReleaseInfo();
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private OrdnungsbegriffSuchTO buildSuchTO(String orbArtSchluessel, String funkArtSchluessel, Integer aMandant) {
        OrdnungsbegriffSuchTO suchTO = new OrdnungsbegriffSuchTO();
        suchTO.setOrbartSchluessel(orbArtSchluessel);
        suchTO.setOrbMandant(aMandant);
        suchTO.setOrbSchluessel(funkArtSchluessel);
        return suchTO;
    }

    private boolean hasVipBerechtigung(String authToken) {
        try {
            return getSSTSecurityToken().isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
        } catch (Exception e) {
            return false;
        }
    }

    private void maskVipPartner(OrdnungsbegriffTO result) {
        for (FunktionTO funktionTO : result.getFunktionen()) {
            String vipKZ = funktionTO.getPartnerTO().getGrunddaten().getVipKennzeichen();
            if (VIPMITARBEITER.equals(vipKZ != null ? vipKZ.trim() : null)) {
                long partnerNr = funktionTO.getPartnerTO().getGrunddaten().getPartnerNr();
                int klient = funktionTO.getPartnerTO().getGrunddaten().getKlient();
                replaceWithMaskedPartner(funktionTO, partnerNr, klient);
            }
        }
    }

    private void maskVipPartnerFull(OrdnungsbegriffServiceTO result) {
        for (FunktionServiceTO funktionTO : result.getFunktionen()) {
            String vipKZ = funktionTO.getPartnerTO().getGrunddaten().getVipKennzeichen();
            if (VIPMITARBEITER.equals(vipKZ != null ? vipKZ.trim() : null)) {
                long partnerNr = funktionTO.getPartnerTO().getGrunddaten().getPartnerNr();
                int klient = funktionTO.getPartnerTO().getGrunddaten().getKlient();
                replaceWithMaskedPartner(funktionTO, partnerNr, klient);
            }
        }
    }

    private void replaceWithMaskedPartner(FunktionTO funktionTO, long partnerNr, int klient) {
        if (funktionTO.getPartnerTO() instanceof NatuerlichePersonTO) {
            funktionTO.setPartnerTO(new NatuerlichePersonTO());
            funktionTO.getPartnerTO().setPartnerTyp("1");
            funktionTO.getPartnerTO().setGrunddaten(new NatuerlichePersonGrunddatenTO());
        } else if (funktionTO.getPartnerTO() instanceof NichtNatuerlichePersonTO) {
            funktionTO.setPartnerTO(new NichtNatuerlichePersonTO());
            funktionTO.getPartnerTO().setPartnerTyp("2");
            funktionTO.getPartnerTO().setGrunddaten(new NichtNatuerlichePersonGrunddatenTO());
        } else if (funktionTO.getPartnerTO() instanceof PartnerGemeinschaftTO) {
            funktionTO.setPartnerTO(new PartnerGemeinschaftTO());
            funktionTO.getPartnerTO().setPartnerTyp("3");
            funktionTO.getPartnerTO().setGrunddaten(new PartnerGemeinschaftGrunddatenTO());
        }
        funktionTO.getPartnerTO().getGrunddaten().setPartnerNr(partnerNr);
        funktionTO.getPartnerTO().getGrunddaten().setKlient(klient);
        funktionTO.getPartnerTO().getGrunddaten().setVipKennzeichen(VIPMITARBEITER);
    }
}

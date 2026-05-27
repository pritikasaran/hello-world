package de.deltalloyd.partnerdb.webservices.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.deltalloyd.foundation.auth.exception.AuthorisationException;
import de.deltalloyd.foundation.exception.AbstractBusinessException;
import de.deltalloyd.foundation.exception.AbstractCheckedSystemException;
import de.deltalloyd.partnerdb.access.exception.DataAccessBusinessException;
import de.deltalloyd.partnerdb.access.exception.PartnerVorgangExistsException;
import de.deltalloyd.partnerdb.access.interfaces.PartnerDataAccess;
import de.deltalloyd.partnerdb.access.interfaces.PartnerDataAccessFactory;
import de.deltalloyd.partnerdb.access.type.BenutzerInfo;
import de.deltalloyd.partnerdb.access.type.FachlicherVorgang;
import de.deltalloyd.partnerdb.access.type.JournalEintrag;
import de.deltalloyd.partnerdb.access.type.KistAbrechnungsverband;
import de.deltalloyd.partnerdb.access.type.NatuerlichePerson;
import de.deltalloyd.partnerdb.access.type.NichtNatuerlichePerson;
import de.deltalloyd.partnerdb.access.type.Partner;
import de.deltalloyd.partnerdb.access.type.PartnerGemeinschaft;
import de.deltalloyd.partnerdb.access.type.jpa.PartnerPK;
import de.deltalloyd.partnerdb.core.exception.CoreContractException;
import de.deltalloyd.partnerdb.core.exception.PartnerInvalidExcepion;
import de.deltalloyd.partnerdb.core.exception.PartnerModifiedException;
import de.deltalloyd.partnerdb.core.exception.PartnerNotFoundException;
import de.deltalloyd.partnerdb.core.impl.PartnerServiceEJB;
import de.deltalloyd.partnerdb.core.monitor.appreg.MyAppPerformanceSensor;
import de.deltalloyd.partnerdb.core.type.AdresseTO;
import de.deltalloyd.partnerdb.core.type.BankVerbindungTO;
import de.deltalloyd.partnerdb.core.type.EinzelVsnrTO;
import de.deltalloyd.partnerdb.core.type.JournalEintragTO;
import de.deltalloyd.partnerdb.core.type.NatuerlichePersonGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.NatuerlichePersonTO;
import de.deltalloyd.partnerdb.core.type.NichtNatuerlichePersonGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.NichtNatuerlichePersonTO;
import de.deltalloyd.partnerdb.core.type.PartnerGemeinschaftGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.PartnerGemeinschaftTO;
import de.deltalloyd.partnerdb.core.type.PartnerGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.PartnerTO;
import de.deltalloyd.partnerdb.core.type.PostAdresseTO;
import de.deltalloyd.partnerdb.core.type.PostPruefAntwortTO;
import de.deltalloyd.partnerdb.core.type.ValidationError;
import de.deltalloyd.partnerdb.core.type.ValidationResult;
import de.deltalloyd.partnerdb.core.type.util.PartnerBilanzdaten;
import de.deltalloyd.partnerdb.core.type.util.PartnerValidator;
import de.deltalloyd.partnerdb.orb.access.exception.OrbAccessNotFoundException;
import de.deltalloyd.partnerdb.process.exception.PartnerManagerBusinessException;
import de.deltalloyd.partnerdb.security.profiles.PdbBerechtigungen;
import de.deltalloyd.partnerdb.security.services.SecurityTokenSevice;
import de.deltalloyd.partnerdb.webservices.exception.PDBException;
import de.deltalloyd.partnerdb.webservices.exception.PartnerWebServiceException;
import de.deltalloyd.partnerdb.webservices.security.exception.STSException;
import de.deltalloyd.partnerdb.webservices.utils.PDBReleaseInfo;

@Stateless
@Interceptors(MyAppPerformanceSensor.class)
public class PartnerService extends AbstractWebService {

    private static final String PARTNER_NICHT_GEFUNDEN = "Partner wurde nicht gefunden.";
    private static final String LADE_PARTNER_BANKVERBINDUNG = "ladePartnerBankverbindung:";
    private static final String LADE_PARTNER_POST_ADRESSE = "ladePartnerPostAdresse:";

    private static final Logger LOG_THIS = LoggerFactory.getLogger(PartnerService.class);

    @PersistenceContext(unitName = "PDBPOOL")
    private EntityManager em;

    @EJB
    private PartnerServiceEJB partnerServiceLocal;

    @EJB
    private SecurityTokenSevice sts;

    private PartnerValidator partnerValidator;
    private PartnerDataAccess dataAccess;

    public PartnerService() {
        partnerValidator = new PartnerValidator();
        dataAccess = new PartnerDataAccessFactory().createPartnerDataAccess();
    }

    @PostConstruct
    public void initEm() {
        dataAccess.registerEntityManager(em);
        partnerValidator.registerDataAccess(dataAccess);
    }

    public PartnerTO ladePartner(final String authToken, final boolean nebendubletteaufloesen,
            final Integer aKlient, final Long aPartnerNr, final String aParameter) throws PartnerWebServiceException {
        try {
            LOG_THIS.debug("ladePartner:Aufruf> klient: " + aKlient + ", partnernr: " + aPartnerNr);
            sts.isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
            LOG_THIS.error("aPartnerNr---webserviceimpl-------------" + aPartnerNr);
            PartnerTO partnerTO = partnerServiceLocal.ladePartner(aKlient, aPartnerNr);
            if (partnerTO == null) {
                LOG_THIS.error("ladeKopfDublettenPartner:" + aKlient + "," + aPartnerNr + "," + aParameter + ":"
                        + PARTNER_NICHT_GEFUNDEN);
                throw new PartnerWebServiceException(
                        new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, null));
            }
            if (nebendubletteaufloesen && partnerTO.getGrunddaten().isNebenDublette()) {
                LOG_THIS.debug("nebendubletteaufloesen fuer PID:" + aPartnerNr);
                long lKopfDublettePartnerNr = partnerServiceLocal.getKopfDublettenPartnerNr(aKlient, aPartnerNr);
                partnerTO = partnerServiceLocal.ladePartner(aKlient, lKopfDublettePartnerNr);
            }
            if ((partnerTO.getGrunddaten().getVipKennzeichen() != null)
                    && (partnerTO.getGrunddaten().getVipKennzeichen().trim().equals("1"))) {
                getSSTSecurityToken().isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
            }
            return partnerTO;
        } catch (STSException e) {
            LOG_THIS.error(e.getMessage(), e);
            throw new PartnerWebServiceException(new PDBException(PDBException.KEINELESERECHTE, e.getUuid()));
        } catch (AuthorisationException e) {
            LOG_THIS.error(e.getMessage());
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        } catch (PartnerWebServiceException e) {
            LOG_THIS.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG_THIS.error(e.getMessage());
            throw new PartnerWebServiceException(new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage(), null));
        }
    }

    public PostAdresseTO ladePartnerPostAdresse(final String authToken, final Integer aKlient, final Long aPartnerNr,
            int aAnschrnr, final String aParameter) throws PartnerWebServiceException {
        LOG_THIS.debug(LADE_PARTNER_POST_ADRESSE + aKlient + "," + aPartnerNr + "," + aAnschrnr + "," + aParameter);
        try {
            sts.isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
        PartnerTO partnerTO;
        try {
            partnerTO = partnerServiceLocal.ladePartner(aKlient, aPartnerNr);
            if (partnerTO == null) {
                LOG_THIS.error(LADE_PARTNER_POST_ADRESSE + aKlient + "," + aPartnerNr + "," + aAnschrnr + ","
                        + aParameter + ":" + PARTNER_NICHT_GEFUNDEN);
                throw new PartnerWebServiceException(
                        new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, null));
            }
            if (partnerTO.getGrunddaten().isNebenDublette()) {
                aAnschrnr = partnerServiceLocal.getKopfDublettenAdressNr(aKlient, aPartnerNr, aAnschrnr);
                LOG_THIS.debug(LADE_PARTNER_POST_ADRESSE + aKlient + "," + aPartnerNr + "," + aAnschrnr + ","
                        + aParameter + ":Anschrnr aus Kopfdublette : " + aAnschrnr);
                long lKopfDublettePartnerNr = partnerServiceLocal.getKopfDublettenPartnerNr(aKlient, aPartnerNr);
                partnerTO = partnerServiceLocal.ladePartner(aKlient, lKopfDublettePartnerNr);
            }
            if ((partnerTO.getGrunddaten().getVipKennzeichen() != null)
                    && (partnerTO.getGrunddaten().getVipKennzeichen().trim().equals("1"))) {
                sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
            }
            if (aAnschrnr == -1) {
                return partnerTO.getPrimaerePostAdresse();
            } else {
                for (PostAdresseTO postAdresseTO : ((List<PostAdresseTO>) partnerTO.getPostAdressen())) {
                    if (postAdresseTO.getNummer() == aAnschrnr) {
                        return postAdresseTO;
                    }
                }
            }
            LOG_THIS.error(LADE_PARTNER_POST_ADRESSE + aKlient + "," + aPartnerNr + "," + aAnschrnr + "," + aParameter
                    + ":PostAdresseTO nicht gefunden.");
            return null;
        } catch (STSException e) {
            LOG_THIS.error(e.getMessage(), e);
            throw new PartnerWebServiceException(new PDBException(PDBException.KEINELESERECHTE, e.getUuid()));
        } catch (AuthorisationException e) {
            LOG_THIS.error(e.getMessage());
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
    }

    public BankVerbindungTO ladePartnerBankverbindung(final String authToken, final Integer aKlient,
            final Long aPartnerNr, int aBankVerbNr, final String aParameter) throws PartnerWebServiceException {
        LOG_THIS.debug(LADE_PARTNER_BANKVERBINDUNG + aKlient + "," + aPartnerNr + "," + aBankVerbNr + "," + aParameter);
        try {
            sts.isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
        PartnerTO partnerTO;
        try {
            partnerTO = partnerServiceLocal.ladePartner(aKlient, aPartnerNr);
            if (partnerTO == null) {
                LOG_THIS.error(LADE_PARTNER_BANKVERBINDUNG + aKlient + "," + aPartnerNr + "," + aBankVerbNr + ","
                        + aParameter + ":" + PARTNER_NICHT_GEFUNDEN);
                throw new PartnerWebServiceException(
                        new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, null));
            }
            if (partnerTO.getGrunddaten().isNebenDublette()) {
                aBankVerbNr = partnerServiceLocal.getKopfDublettenBankVerbNr(aKlient, aPartnerNr, aBankVerbNr);
                LOG_THIS.debug(LADE_PARTNER_BANKVERBINDUNG + aKlient + "," + aPartnerNr + "," + aBankVerbNr + ","
                        + aParameter + ":AdressNr aus Kopfdublette : " + aBankVerbNr);
                long lKopfDublettePartnerNr = partnerServiceLocal.getKopfDublettenPartnerNr(aKlient, aPartnerNr);
                partnerTO = partnerServiceLocal.ladePartner(aKlient, lKopfDublettePartnerNr);
            }
            if ((partnerTO.getGrunddaten().getVipKennzeichen() != null)
                    && (partnerTO.getGrunddaten().getVipKennzeichen().trim().equals("1"))) {
                sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
            }
            if (aBankVerbNr == -1) {
                return partnerTO.getPrimaereBankVerbindung();
            } else {
                for (BankVerbindungTO bTO : ((List<BankVerbindungTO>) partnerTO.getBankVerbindungen())) {
                    if (bTO.getNummer() == aBankVerbNr) {
                        return bTO;
                    }
                }
            }
            LOG_THIS.error(LADE_PARTNER_BANKVERBINDUNG + aKlient + "," + aPartnerNr + "," + aBankVerbNr + ","
                    + aParameter + ":BankVerbindungTO nicht gefunden.");
            return null;
        } catch (STSException e) {
            LOG_THIS.error(e.getMessage(), e);
            throw new PartnerWebServiceException(new PDBException(PDBException.KEINELESERECHTE, e.getUuid()));
        } catch (AuthorisationException e) {
            LOG_THIS.error(e.getMessage());
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
    }

    public void aktualisierePartner(final String authToken, final PartnerTO partnerTo, final String aParameter)
            throws PartnerWebServiceException {
        FachlicherVorgang vVorgang = null;
        LOG_THIS.info("PartnerService:aktualisierePartner.....");
        boolean errorUpdate = true;
        try {
            sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);
            if (partnerTo == null || partnerTo.getGrunddaten() == null) {
                throw new CoreContractException("aPartnerTO und aPartnerTO.grunddaten duerfen nicht null sein");
            }
            LOG_THIS.info("PartnerService:aktualisierePartner....." + partnerTo.getPartnerNr());
            if ((partnerTo.getGrunddaten().getVipKennzeichen() != null)
                    && (partnerTo.getGrunddaten().getVipKennzeichen().trim().equals("1"))) {
                sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
            }
            Partner vPartner = dataAccess.findPartnerByID(partnerTo.getMandant(), partnerTo.getPartnerNr());
            ValidationResult vValidationResult = partnerValidator.validiereGrunddaten(partnerTo.getGrunddaten());
            if (vValidationResult.isValidationOkay()) {
                vValidationResult = partnerValidator.validiereUebergreifend(partnerTo);
            }
            if (!vValidationResult.isValidationOkay()) {
                if (vValidationResult.getValidationErrors() != null) {
                    for (ValidationError error : vValidationResult.getValidationErrors()) {
                        LOG_THIS.error("aktualisierePartner Error-Key: {}, {}", error.getReasonKey(),
                                Arrays.toString(error.getAttributeKeys()));
                    }
                }
                throw new PartnerInvalidExcepion(vValidationResult);
            }
            String sUserID = getSSTSecurityToken().getUserId(authToken);
            vVorgang = new FachlicherVorgang(em);
            vVorgang.setOrb(partnerTo.getPartnerNr().longValue());
            vVorgang.setKlient(partnerTo.getMandant().intValue());
            vVorgang.setUser(sUserID);
            vVorgang.start();
            if (vPartner == null) {
                throw new PartnerNotFoundException(partnerTo.getMandant() + "," + partnerTo.getPartnerNr());
            }
            BenutzerInfo benutzerInfo = dataAccess.findBenutzerInfoByID(sUserID);
            JournalEintrag[] vJournalEintrags = vPartner.aktualisiereVonTO(partnerTo, sUserID,
                    benutzerInfo.getBezeichnung(), 0);
            dataAccess.updateSearchIndex(partnerTo);
            errorUpdate = false;
        } catch (STSException e) {
            LOG_THIS.error(e.getMessage(), e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(
                    new PDBException(PDBException.KEINESCHREIBRECHTE, e.getUuid()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } catch (PartnerVorgangExistsException e) {
            logException(e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(
                    new PDBException(PDBException.PartnerVorgangExistsException, e.getMessage(), e.getUuid()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } catch (CoreContractException e) {
            logException(e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(
                    new PDBException(PDBException.CoreContractException, e.getMessage(), e.getUuid()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } catch (PartnerInvalidExcepion e) {
            logException(e);
            String sErrors = buildValidationErrors(e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(new PDBException(
                    PDBException.PartnerInvalidExcepion, e.getMessage() + " (" + sErrors + ")", e.getUuid()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } catch (PartnerNotFoundException e) {
            logException(e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(
                    new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, e.getUuid()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } catch (PartnerModifiedException e) {
            logException(e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(
                    new PDBException(PDBException.PartnerModifiedException, e.getMessage(), e.getUuid()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } catch (Exception e) {
            LOG_THIS.error("Exception vom Typ " + e.getClass());
            LOG_THIS.error(e.getMessage(), e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(
                    new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } finally {
            fachlicherVorgangSchliessen(vVorgang, errorUpdate);
        }
    }

    public String getReleaseInfo() {
        return PDBReleaseInfo.getReleaseInfo();
    }

    public ValidationResult validiereAdresse(PartnerTO aPartnerTO, AdresseTO aAdresseTO, String aParameter)
            throws PartnerWebServiceException {
        try {
            return partnerServiceLocal.validiereAdresse(aPartnerTO, aAdresseTO);
        } catch (AuthorisationException e) {
            LOG_THIS.error(e.getMessage());
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
    }

    public ValidationResult validiereBankverbindung(PartnerTO aPartnerTO, BankVerbindungTO aBankverbindung,
            String aParameter) throws PartnerWebServiceException {
        try {
            return partnerServiceLocal.validiereBankverbindung(aPartnerTO, aBankverbindung);
        } catch (AuthorisationException e) {
            LOG_THIS.error(e.getMessage());
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
    }

    public ValidationResult validiereGrunddaten(PartnerGrunddatenTO aTO, String aParameter)
            throws PartnerWebServiceException {
        try {
            return partnerServiceLocal.validiereGrunddaten(aTO);
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        } catch (CoreContractException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.CoreContractException, e.getMessage(), e.getUuid()));
        }
    }

    public ValidationResult validiereUebergreifend(PartnerTO aTO, String aParameter) throws PartnerWebServiceException {
        try {
            return partnerValidator.validiereUebergreifend(aTO);
        } catch (CoreContractException e) {
            LOG_THIS.error(e.getMessage());
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.CoreContractException, e.getMessage(), e.getUuid()));
        }
    }

    public PostPruefAntwortTO pruefePostAdresse(final PostAdresseTO aPostadresse, final String aParameter)
            throws PartnerWebServiceException {
        try {
            return partnerServiceLocal.pruefePostAdresse(aPostadresse);
        } catch (PartnerManagerBusinessException e) {
            LOG_THIS.error(e.getMessage());
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.PartnerManagerBusinessException, e.getMessage(), e.getUuid()));
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
    }

    public void vorgangEntsprerren(final String authToken, final int klient, final long vorgang)
            throws PartnerWebServiceException {
        try {
            sts.isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
            dataAccess.removeVorgang(klient, vorgang);
        } catch (STSException | AbstractCheckedSystemException e) {
            LOG_THIS.error(e.getMessage(), e);
            throw new PartnerWebServiceException(new PDBException(PDBException.KEINESCHREIBRECHTE, e.getUuid()));
        }
    }

    public long holeFreiePartnerNummer(final String authToken, final int aKlient, final String aParameter)
            throws PartnerWebServiceException {
        try {
            sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);
            return partnerServiceLocal.holeFreiePartnerNummer(aKlient);
        } catch (STSException e) {
            LOG_THIS.error(e.getMessage(), e);
            throw new PartnerWebServiceException(new PDBException(PDBException.KEINESCHREIBRECHTE, e.getUuid()));
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
    }

    public ValidationResult erzeugePartner(final String authToken, final PartnerTO aPartnerTO, final String aParameter)
            throws PartnerWebServiceException {
        ValidationResult vValidationResult;
        try {
            sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);
            vValidationResult = partnerValidator.validiereUebergreifend(aPartnerTO);
            if (!vValidationResult.isValidationOkay()) {
                return vValidationResult;
            }
            Partner vPartner = null;
            if (aPartnerTO instanceof NichtNatuerlichePersonTO) {
                vPartner = new NichtNatuerlichePerson();
            } else if (aPartnerTO instanceof NatuerlichePersonTO) {
                vPartner = new NatuerlichePerson();
            } else if (aPartnerTO instanceof PartnerGemeinschaftTO) {
                vPartner = new PartnerGemeinschaft();
            }
            PartnerPK id = new PartnerPK(aPartnerTO.getMandant().intValue(), aPartnerTO.getPartnerNr().longValue());
            if (vPartner != null) {
                vPartner.setId(id);
            } else {
                LOG_THIS.error("vPartner ist unerwartet null");
                throw new NullPointerException();
            }
            String sUserID = getSSTSecurityToken().getUserId(authToken);
            BenutzerInfo benutzerInfo = dataAccess.findBenutzerInfoByID(sUserID);
            try {
                vPartner.aktualisiereVonTO(aPartnerTO, sUserID, benutzerInfo.getBezeichnung(),
                        benutzerInfo.getKostenstelle());
            } catch (PartnerModifiedException e) {
                LOG_THIS.error("Kann nicht auftreten, da Neuanlage das Partners");
                logException(e);
            }
            dataAccess.createPartner(vPartner);
            dataAccess.insertIntoSearchIndex(aPartnerTO);
            return vValidationResult;
        } catch (STSException e) {
            LOG_THIS.error(e.getMessage(), e);
            throw new PartnerWebServiceException(new PDBException(PDBException.KEINESCHREIBRECHTE, e.getUuid()));
        } catch (CoreContractException e) {
            logException(e);
            LOG_THIS.error(e.getMessage(), e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.CoreContractException, e.getMessage(), e.getUuid()));
        } catch (Exception e) {
            LOG_THIS.error(e.getMessage(), e);
            throw new PartnerWebServiceException(new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage()));
        }
    }

    public int getKopfDublettenAdressNr(int aKlient, long aPartnerNr, int aAdressNr) throws PartnerWebServiceException {
        try {
            return partnerServiceLocal.getKopfDublettenAdressNr(aKlient, aPartnerNr, aAdressNr);
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
    }

    public int getKopfDublettenBankVerbNr(int aKlient, long aPartnerNr, int aBankVerbNr)
            throws PartnerWebServiceException {
        try {
            return partnerServiceLocal.getKopfDublettenBankVerbNr(aKlient, aPartnerNr, aBankVerbNr);
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
    }

    public long getKopfDublettenPartnerNr(int aKlient, long aPartnerNr) throws PartnerWebServiceException {
        try {
            return partnerServiceLocal.getKopfDublettenPartnerNr(aKlient, aPartnerNr);
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
    }

    public List<JournalEintragTO> ladeJournalFuerPartner(int aKlient, long aPartnerNummer)
            throws PartnerWebServiceException {
        try {
            return partnerServiceLocal.ladeJournalFuerPartner(aKlient, aPartnerNummer);
        } catch (PartnerNotFoundException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, e.getUuid()));
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
    }

    public List<String[]> ladeSapPartnerInformation(String aPID) throws PartnerWebServiceException {
        List<String[]> list = new ArrayList<>();
        Properties p = partnerServiceLocal.ladeSapPartnerInformation(aPID);
        for (Enumeration en = p.keys(); en.hasMoreElements();) {
            String sKey = (String) en.nextElement();
            String sValue = p.getProperty(sKey);
            String[] sValuePair = { sKey, sValue };
            list.add(sValuePair);
        }
        return list;
    }

    public PartnerBilanzdaten getbilanzdaten(final Integer aKlient, final Long aPartnerNr)
            throws PartnerWebServiceException {
        PartnerBilanzdaten partnerbilanzdaten = new PartnerBilanzdaten();
        PartnerTO partnerto;
        try {
            partnerto = partnerServiceLocal.ladePartner(aKlient, aPartnerNr);
        } catch (AuthorisationException e) {
            LOG_THIS.error(e.getMessage());
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
        if (partnerto == null) {
            LOG_THIS.error("Partner wurde nicht gefunden (" + aKlient + ")(" + aPartnerNr + ").");
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, null));
        } else if (partnerto.getGrunddaten() instanceof NichtNatuerlichePersonGrunddatenTO) {
            partnerbilanzdaten.setBilanzMonat(
                    ((NichtNatuerlichePersonGrunddatenTO) partnerto.getGrunddaten()).getBilanzMonat());
            partnerbilanzdaten.setAbwVersandMonat(
                    ((NichtNatuerlichePersonGrunddatenTO) partnerto.getGrunddaten()).getAbwVersand());
            partnerbilanzdaten.setPartnerNr(aPartnerNr);
            partnerbilanzdaten.setMandant(partnerto.getMandant());
        } else if (partnerto.getGrunddaten() instanceof PartnerGemeinschaftGrunddatenTO) {
            partnerbilanzdaten.setBilanzMonat(
                    ((PartnerGemeinschaftGrunddatenTO) partnerto.getGrunddaten()).getBilanzMonat());
            partnerbilanzdaten.setAbwVersandMonat(
                    ((PartnerGemeinschaftGrunddatenTO) partnerto.getGrunddaten()).getAbwVersand());
            partnerbilanzdaten.setPartnerNr(aPartnerNr);
            partnerbilanzdaten.setMandant(partnerto.getMandant());
        } else {
            LOG_THIS.error("Partnertyp ungueltig. (" + aKlient + ")(" + aPartnerNr + ").");
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.DATENFEHLER, "Partnertyp ungueltig.", null));
        }
        return partnerbilanzdaten;
    }

    public List<EinzelVsnrTO> getNeusteEinzelVsnr(final String authToken, final Integer mandant,
            final String orbart, final String orb) throws PartnerWebServiceException {
        LOG_THIS.debug("getNeusteEinzelVsnr-Abfrage. token:" + authToken + " mandant:" + mandant + " orb:" + orb);
        try {
            sts.isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
            return partnerServiceLocal.getNeusteEinzelVsnr(mandant, orbart, orb);
        } catch (STSException e) {
            LOG_THIS.error(e.getMessage(), e);
            throw new PartnerWebServiceException(new PDBException(PDBException.KEINELESERECHTE, e.getUuid()));
        } catch (OrbAccessNotFoundException e) {
            LOG_THIS.error(e.getMessage(), e);
            throw new PartnerWebServiceException(new PDBException(PDBException.DATENFEHLER, e.getMessage(), ""));
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(new PDBException(PDBException.AbstractCheckedSystemException,
                    "ORB wurde nicht gefunden. " + e.getMessage(), ""));
        }
    }

    public List<KistAbrechnungsverband> ladeKiStAbrechnungVerbaende() throws PartnerWebServiceException {
        try {
            return partnerServiceLocal.ladeKiStAbrechnungVerbaende();
        } catch (AbstractCheckedSystemException e) {
            logException(e);
            throw new PartnerWebServiceException(
                    new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
        }
    }

    public void updateUebertragungBRS(final String authToken, int klient, long partnerNr, String brsUebertragung)
            throws PartnerWebServiceException {
        try {
            sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);
            String userId = getSSTSecurityToken().getUserId(authToken);
            partnerServiceLocal.updateBrsUebertragung(userId, klient, partnerNr, brsUebertragung);
        } catch (STSException e) {
            LOG_THIS.error(e.getMessage(), e);
            throw new PartnerWebServiceException(new PDBException(PDBException.KEINESCHREIBRECHTE, e.getUuid()));
        } catch (AbstractCheckedSystemException | PartnerManagerBusinessException e) {
            LOG_THIS.error(e.getMessage(), e);
            throw new PartnerWebServiceException(new PDBException(PDBException.PartnerManagerBusinessException,
                    "Fehler beim Bearbeiten des Partners. " + e.getMessage(), ""));
        } catch (Exception e) {
            LOG_THIS.error("updateUebertragungBRS error", e);
            throw new PartnerWebServiceException(new PDBException(PDBException.PartnerManagerBusinessException,
                    "Unerwartete Fehler: " + e.getMessage()));
        }
    }

    public List<Object> createPassword(final String authToken, final Integer aKlient, final Long aPartnerNr,
            final String mail) throws PartnerWebServiceException {
        boolean errorUpdate = true;
        FachlicherVorgang vVorgang = null;
        List<Object> responseList;
        try {
            sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);
            PartnerTO partnerTo = partnerServiceLocal.ladePartner(aKlient, aPartnerNr);
            if (partnerTo == null || partnerTo.getGrunddaten() == null) {
                throw new CoreContractException("aPartnerTO und aPartnerTO.grunddaten duerfen nicht null sein");
            }
            LOG_THIS.info("PartnerService:createPassword....." + partnerTo.getPartnerNr());
            if ((partnerTo.getGrunddaten().getVipKennzeichen() != null)
                    && (partnerTo.getGrunddaten().getVipKennzeichen().trim().equals("1"))) {
                sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
            }
            responseList = partnerServiceLocal.updatePaswordandBroker(partnerTo, mail);
            Partner vPartner = dataAccess.findPartnerByID(partnerTo.getMandant(), partnerTo.getPartnerNr());
            ValidationResult vValidationResult = partnerValidator.validiereGrunddaten(partnerTo.getGrunddaten());
            if (vValidationResult.isValidationOkay()) {
                vValidationResult = partnerValidator.validiereUebergreifend(partnerTo);
            }
            if (!vValidationResult.isValidationOkay()) {
                if (vValidationResult.getValidationErrors() != null) {
                    for (ValidationError error : vValidationResult.getValidationErrors()) {
                        LOG_THIS.error("aktualisierePartner Error-Key: {}, {}", error.getReasonKey(),
                                Arrays.toString(error.getAttributeKeys()));
                    }
                }
                throw new PartnerInvalidExcepion(vValidationResult);
            }
            String sUserID = getSSTSecurityToken().getUserId(authToken);
            vVorgang = new FachlicherVorgang(em);
            vVorgang.setOrb(partnerTo.getPartnerNr().longValue());
            vVorgang.setKlient(partnerTo.getMandant().intValue());
            vVorgang.setUser(sUserID);
            vVorgang.start();
            if (vPartner == null) {
                throw new PartnerNotFoundException(partnerTo.getMandant() + "," + partnerTo.getPartnerNr());
            }
            BenutzerInfo benutzerInfo = dataAccess.findBenutzerInfoByID(sUserID);
            JournalEintrag[] vJournalEintrags = vPartner.aktualisiereVonTO(partnerTo, sUserID,
                    benutzerInfo.getBezeichnung(), 0);
            dataAccess.updateSearchIndex(partnerTo);
            errorUpdate = false;
        } catch (STSException e) {
            LOG_THIS.error(e.getMessage(), e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(
                    new PDBException(PDBException.KEINESCHREIBRECHTE, e.getUuid()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } catch (PartnerVorgangExistsException e) {
            logException(e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(
                    new PDBException(PDBException.PartnerVorgangExistsException, e.getMessage(), e.getUuid()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } catch (CoreContractException e) {
            logException(e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(
                    new PDBException(PDBException.CoreContractException, e.getMessage(), e.getUuid()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } catch (PartnerInvalidExcepion e) {
            logException(e);
            String sErrors = buildValidationErrors(e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(new PDBException(
                    PDBException.PartnerInvalidExcepion, e.getMessage() + " (" + sErrors + ")", e.getUuid()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } catch (PartnerNotFoundException e) {
            logException(e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(
                    new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, e.getUuid()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } catch (PartnerModifiedException e) {
            logException(e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(
                    new PDBException(PDBException.PartnerModifiedException, e.getMessage(), e.getUuid()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } catch (Exception e) {
            LOG_THIS.error("Exception vom Typ " + e.getClass());
            LOG_THIS.error(e.getMessage(), e);
            PartnerWebServiceException vEx = new PartnerWebServiceException(
                    new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage()));
            LOG_THIS.error(vEx.getMessage(), vEx);
            throw vEx;
        } finally {
            fachlicherVorgangSchliessen(vVorgang, errorUpdate);
        }
        return responseList;
    }

    // ── private helpers (same as original) ────────────────────────────────────

    private void fachlicherVorgangSchliessen(FachlicherVorgang vVorgang, boolean errorUpdate)
            throws PartnerWebServiceException {
        if (vVorgang != null && vVorgang.isStarted()) {
            if (errorUpdate) {
                try {
                    vVorgang.end();
                } catch (Exception e) {
                    LOG_THIS.error(e.getMessage(), e);
                }
            } else {
                try {
                    vVorgang.end();
                } catch (RuntimeException e) {
                    LOG_THIS.error(e.getMessage(), e);
                    PartnerWebServiceException vEx = new PartnerWebServiceException(
                            new PDBException(PDBException.RuntimeException, e.getMessage(), null));
                    LOG_THIS.error(vEx.getMessage(), vEx);
                    throw vEx;
                } catch (DataAccessBusinessException e) {
                    logException(e);
                    PartnerWebServiceException vEx = new PartnerWebServiceException(
                            new PDBException(PDBException.DataAccessBusinessException, e.getMessage(), e.getUuid()));
                    LOG_THIS.error(vEx.getMessage(), vEx);
                    throw vEx;
                }
            }
        }
    }

    private String buildValidationErrors(PartnerInvalidExcepion e) {
        String sErrors = "";
        if ((e.getValidationResult() != null) && (e.getValidationResult().getValidationErrors() != null)) {
            for (ValidationError vE : e.getValidationResult().getValidationErrors()) {
                String sAttributes = "";
                if (vE.getAttributeKeys() != null) {
                    for (String s : vE.getAttributeKeys()) {
                        sAttributes += s + ",";
                    }
                }
                if (sAttributes.length() > 0) {
                    sAttributes = sAttributes.substring(0, sAttributes.length() - 1);
                }
                sErrors += vE.getReasonKey() + " : " + sAttributes + ";";
            }
        }
        return sErrors;
    }

    private void logException(final AbstractBusinessException aEx) {
        LOG_THIS.error("AbstractBusinessException");
        LOG_THIS.error(aEx.getMessage(), aEx);
    }

    private void logException(final AbstractCheckedSystemException aEx) {
        LOG_THIS.error("AbstractCheckedSystemException");
        LOG_THIS.error(aEx.getMessage(), aEx);
    }
}

package de.deltalloyd.partnerdb.webservices.impl;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.resource.ResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.deltalloyd.foundation.auth.exception.PolicyCheckException;
import de.deltalloyd.foundation.exception.AbstractCheckedSystemException;
import de.deltalloyd.partnerdb.core.exception.CoreSystemException;
import de.deltalloyd.partnerdb.core.monitor.appreg.MyAppPerformanceSensor;
import de.deltalloyd.partnerdb.core.type.kksn.KksnTO;
import de.deltalloyd.partnerdb.core.type.kksn.Kksnw2TO;
import de.deltalloyd.partnerdb.core.type.kksn.Kksnw3TO;
import de.deltalloyd.partnerdb.core.type.kksn.Kksnw4TO;
import de.deltalloyd.partnerdb.core.type.kksn.StKksnFunk2AktTO;
import de.deltalloyd.partnerdb.core.type.kksn.StKksnOrbAktTO;
import de.deltalloyd.partnerdb.core.type.kksn.StKksnOrbLesTO;
import de.deltalloyd.partnerdb.core.type.kksn.StKksnPersLesTO;
import de.deltalloyd.partnerdb.core.type.kksn.StKksnVorVersTO;
import de.deltalloyd.partnerdb.security.profiles.PdbBerechtigungen;
import de.deltalloyd.partnerdb.security.services.SecurityTokenSevice;
import de.deltalloyd.partnerdb.webservices.exception.KksnFehler;
import de.deltalloyd.partnerdb.webservices.utils.WSParams;
import de.deltalloyd.uniservra.UniservConnection;
import de.deltalloyd.uniservra.UniservConnectionFactory;
import de.deltalloyd.uniservra.UniservPostAllResultTO;
import de.deltalloyd.uniservra.UniservPostAllTO;

@Stateless
@Interceptors(MyAppPerformanceSensor.class)
public class KksnService extends KksnBasisWS {

    private static final Logger LOGGER = LoggerFactory.getLogger(KksnService.class);
    private static final Integer VIPMITARBEITER = 1;
    private final int defaultClient = 100;

    @EJB
    private SecurityTokenSevice sts;

    @Resource(name = "jca/partner/core/uniservpost")
    private UniservConnectionFactory uniservPostConnectionFactory;

    public KksnTO kksnPersLes(final String authToken, final StKksnPersLesTO pPar, final String db) {
        StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
        KksnTO kksnTo = null;
        try {
            checkKlient(par);
            kksnTo = checkParams(new Object[] { authToken, par, par.getKlient(), par.getPersNr() }, WSParams.authToken,
                    WSParams.stKksnPersLes, WSParams.klient, WSParams.partnerNr);
            if (kksnTo == null) {
                kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
                if (kksnTo.getPrc() == KksnFehler.ENUL) {
                    kksnTo = kksnWorker.kksnPersLes(par, db);
                    kksnTo = checkVipMitarbeiterPolicy(authToken, kksnTo);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Fehler in kksnPersLes. ", e);
            kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
        }
        return kksnTo;
    }

    public KksnTO kksnDubrefK(final String authToken, final StKksnPersLesTO pPar, final String db) {
        StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
        KksnTO kksnTo = null;
        try {
            if (par.getAnschrNr() == null) {
                par.setAnschrNr(0);
            }
            if (par.getBankverbindNr() == null) {
                par.setBankverbindNr(0);
            }
            checkKlient(par);
            kksnTo = checkParams(
                    new Object[] { authToken, par.getKlient(), par.getPersNr(), par.getAnschrNr(),
                            par.getBankverbindNr() },
                    WSParams.authToken, WSParams.klient, WSParams.partnerNr, WSParams.anschriftnr,
                    WSParams.bankverbindnr);
            if (kksnTo == null) {
                kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
                if (kksnTo.getPrc() == KksnFehler.ENUL) {
                    kksnTo = kksnWorker.kksnDubrefK(par, db);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Fehler in kksnDubrefK. ", e);
            kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
        }
        return kksnTo;
    }

    public KksnTO kksnPersnrPruef(final String authToken, final StKksnPersLesTO pPar, final String db) {
        StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
        KksnTO kksnTo = null;
        try {
            checkKlient(par);
            kksnTo = checkParams(new Object[] { authToken, par.getKlient(), par.getMandant(), par.getPersNr() },
                    WSParams.authToken, WSParams.klient, WSParams.mandant, WSParams.partnerNr);
            if (kksnTo == null) {
                kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
                if (kksnTo.getPrc() == KksnFehler.ENUL) {
                    kksnTo = kksnWorker.kksnPersnrPruef(par, db);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Fehler in kksnPersnrPruef. ", e);
            kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
        }
        return kksnTo;
    }

    public KksnTO kksnAnLes(final String authToken, final StKksnPersLesTO pPar, final String db) {
        StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
        KksnTO kksnTo = null;
        checkKlient(par);
        try {
            kksnTo = checkParams(new Object[] { authToken, par.getKlient(), par.getPersNr(), par.getAnschrNr() },
                    WSParams.authToken, WSParams.klient, WSParams.partnerNr, WSParams.anschriftnr_0);
            if (kksnTo == null) {
                kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
                if (kksnTo.getPrc() == KksnFehler.ENUL) {
                    kksnTo = kksnWorker.kksnAnLes(par, db);
                    kksnTo = checkVipMitarbeiterPolicy(authToken, kksnTo);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Fehler in kksnAnLes. ", e);
            kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
        }
        return kksnTo;
    }

    public KksnTO kksnBaLes(final String authToken, final StKksnPersLesTO pPar, final String db) {
        StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
        KksnTO kksnTo = null;
        try {
            checkKlient(par);
            kksnTo = checkParams(new Object[] { authToken, par.getKlient(), par.getPersNr(), par.getBankverbindNr() },
                    WSParams.authToken, WSParams.klient, WSParams.partnerNr, WSParams.bankverbindnr_0);
            if (kksnTo == null) {
                kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
                if (kksnTo.getPrc() == KksnFehler.ENUL) {
                    kksnTo = kksnWorker.kksnBaLes(par, db);
                    kksnTo = checkVipMitarbeiterPolicy(authToken, kksnTo);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Fehler in kksnBaLes. ", e);
            kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
        }
        return kksnTo;
    }

    public KksnTO kksnVorVers(final String authToken, final StKksnPersLesTO pPar, final String db) {
        StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
        KksnTO kksnTo = null;
        try {
            checkKlient(par);
            kksnTo = checkParams(new Object[] { authToken, par.getKlient(), par.getPersNr() }, WSParams.authToken,
                    WSParams.klient, WSParams.partnerNr);
            if (kksnTo == null) {
                kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
                if (kksnTo.getPrc() == KksnFehler.ENUL) {
                    kksnTo = kksnWorker.kksnVorVers(par, db);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Fehler in kksnVorVers. ", e);
            kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
        }
        return kksnTo;
    }

    public KksnTO kksnVorVersVN(final String authToken, final StKksnPersLesTO pPar, final String db) {
        StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
        KksnTO kksnTo = null;
        try {
            checkKlient(par);
            kksnTo = checkParams(new Object[] { authToken, par.getKlient(), par.getPersNr() }, WSParams.authToken,
                    WSParams.klient, WSParams.partnerNr);
            if (kksnTo == null) {
                kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
                if (kksnTo.getPrc() == KksnFehler.ENUL) {
                    kksnTo = kksnWorker.kksnVorVersVN(par, db);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Fehler in kksnVorVersVN. ", e);
            kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
        }
        return kksnTo;
    }

    public KksnTO pdsnPidZuBilmon(final String authToken, final Integer klient, final Integer bilMonat,
            final String db) {
        KksnTO kksnTo = null;
        try {
            kksnTo = checkParams(new Object[] { authToken, klient, bilMonat }, WSParams.authToken, WSParams.klient,
                    WSParams.bilMonat);
            if (kksnTo == null) {
                kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
                if (kksnTo.getPrc() == KksnFehler.ENUL) {
                    kksnTo = kksnWorker.pdsnPidZuBilmon(klient, bilMonat, db);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Fehler in pdsnPidZuBilmon. ", e);
            kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
        }
        return kksnTo;
    }

    public KksnTO kksnFunk2akt(final String authToken, final String userId, final Integer pfc,
            final StKksnFunk2AktTO pAlt, final StKksnFunk2AktTO pNeu, final String db) {
        KksnTO kksnTo = null;
        try {
            kksnTo = checkPolicy(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);
            if (kksnTo.getPrc() == KksnFehler.ENUL) {
                kksnTo = orbWorker.kksnFunk2akt(userId, pfc, pAlt, pNeu, db);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Fehler in kksnFunk2akt. ", e);
            kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
        }
        return kksnTo;
    }

    public KksnTO kksnOrbAkt(final String authToken, final StKksnOrbAktTO pPar, final String db) {
        StKksnOrbAktTO par = pPar == null ? new StKksnOrbAktTO() : pPar;
        KksnTO kksnTo = null;
        try {
            kksnTo = checkPolicy(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);
            if (kksnTo.getPrc() == KksnFehler.ENUL) {
                kksnTo = orbWorker.kksnOrbAkt(par, db);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Fehler in kksnOrbAkt. ", e);
            kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
        }
        return kksnTo;
    }

    public KksnTO kksnDubletten(final String authToken, final StKksnPersLesTO pPar, final String db) {
        StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
        KksnTO kksnTo = null;
        try {
            checkKlient(par);
            kksnTo = checkParams(new Object[] { authToken, par.getKlient(), par.getPersNr() }, WSParams.authToken,
                    WSParams.klient, WSParams.partnerNr);
            if (kksnTo == null) {
                kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
                if (kksnTo.getPrc() == KksnFehler.ENUL) {
                    kksnTo = kksnWorker.kksnDubletten(par, db);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Fehler in kksnDubletten. ", e);
            kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
        }
        return kksnTo;
    }

    public UniservPostAllResultTO uniPostAll(final String authToken, final UniservPostAllTO pPar, final String db) {
        UniservPostAllResultTO uniPostResult = new UniservPostAllResultTO();
        UniservConnection uniConn = null;
        try {
            uniConn = (UniservConnection) uniservPostConnectionFactory.getConnection();
        } catch (Exception e) {
            LOGGER.error("Exception in uniPostAll - getConnection(): ", e);
            uniPostResult.setMessage(e.getMessage());
        }
        if (uniConn == null) {
            LOGGER.error(" uniConn=Null in uniPostAll - nach getConnection(): ");
        } else {
            try {
                KksnTO kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
                if (kksnTo.getPrc() == KksnFehler.ENUL) {
                    uniPostResult = uniConn.pruefePostAdresse(pPar);
                }
            } catch (Exception e) {
                if (uniPostResult != null)
                    uniPostResult.setMessage(e.getMessage());
                LOGGER.error("Exception in uniPostAll - checkPolicy / pruefePostAdresse: ", e);
                LOGGER.error("uniConn      : " + uniConn);
                LOGGER.error("pPar         : " + pPar);
                LOGGER.error("uniPostResult: " + uniPostResult);
            }
            try {
                uniConn.close();
            } catch (ResourceException e) {
                LOGGER.info("Exception in uniPostAll - close(): ", e);
                uniPostResult.setMessage(e.getMessage());
            }
        }
        return uniPostResult;
    }

    // ── private helpers (same as original) ────────────────────────────────────

    private KksnTO checkPolicy(String authToken, PdbBerechtigungen securityProfile) throws CoreSystemException {
        KksnTO result = new KksnTO();
        try {
            if (!sts.isAllowed(authToken, securityProfile)) {
                result.setPrc(KksnFehler.EBRT, "Der Aufrufer darf die Daten nicht lesen");
            }
        } catch (PolicyCheckException e) {
            LOGGER.error("checkPolicy error", e);
            result.setPrc(KksnFehler.EBRT,
                    "PolicyName '" + securityProfile.toString() + "' existiert nicht. " + e.getMessage());
        } catch (AbstractCheckedSystemException e) {
            LOGGER.error("checkPolicy error", e);
            result.setPrc(KksnFehler.ETOK, "Token ist ungueltig. " + e.getMessage());
        }
        return result;
    }

    private KksnTO checkVipMitarbeiterPolicy(String authToken, KksnTO kksnTo) {
        KksnTO result = kksnTo;
        if (kksnTo == null) {
            return result;
        }
        if ((kksnTo.getVip() != null) && (kksnTo.getVip() == VIPMITARBEITER)) {
            KksnTO check = checkPolicy(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
            if (check.getPrc() != KksnFehler.ENUL) {
                if (check.getPrc() == KksnFehler.EBRT) {
                    check.setPrc(KksnFehler.EVIP, "VIP-Berechtigung fehlt.");
                }
                result = check;
            }
        }
        return result;
    }

    private KksnTO checkParams(Object[] par, WSParams... params) {
        KksnTO result = null;
        for (int i = 0; i < params.length; i++) {
            Object obj = par[i];
            if (WSParams.authToken == params[i]) {
                if ((obj == null) || (((String) obj).length() == 0)) {
                    result = new KksnTO(KksnFehler.EPAR, WSParams.authToken, obj);
                    break;
                }
            } else if (WSParams.klient == params[i]) {
                if ((obj == null) || (((Integer) obj) <= 0)) {
                    result = new KksnTO(KksnFehler.EPAR, WSParams.klient, obj);
                    break;
                }
            } else if (WSParams.partnerNr == params[i]) {
                if ((obj == null) || (((Long) obj) <= 0)) {
                    result = new KksnTO(KksnFehler.EPAR, WSParams.partnerNr, obj);
                    break;
                }
            } else if (WSParams.anschriftnr_0 == params[i]) {
                if ((obj == null) || (((Integer) obj) == 0)) {
                    result = new KksnTO(KksnFehler.EPAR, WSParams.anschriftnr, obj);
                    break;
                }
            } else if (WSParams.anschriftnr == params[i]) {
                if ((obj == null) || (((Integer) obj) < -1)) {
                    result = new KksnTO(KksnFehler.EPAR, WSParams.anschriftnr, obj);
                    break;
                }
            } else if (WSParams.stKksnPersLes == params[i]) {
                if (obj == null) {
                    result = new KksnTO(KksnFehler.EPAR, WSParams.stKksnPersLes, obj);
                }
            } else if (WSParams.bankverbindnr_0 == params[i]) {
                if ((obj == null) || (((Integer) obj) == 0)) {
                    result = new KksnTO(KksnFehler.EPAR, WSParams.bankverbindnr, obj);
                    break;
                }
            } else if (WSParams.bankverbindnr == params[i]) {
                if ((obj == null) || (((Integer) obj) < 0)) {
                    result = new KksnTO(KksnFehler.EPAR, WSParams.bankverbindnr, obj);
                    break;
                }
            } else if (WSParams.mandant == params[i]) {
                if (obj == null) {
                    result = new KksnTO(KksnFehler.EPAR, WSParams.mandant, obj);
                    break;
                }
            } else if (WSParams.orbArt == params[i]) {
                if (obj == null) {
                    result = new KksnTO(KksnFehler.EPAR, WSParams.orbArt, obj);
                    break;
                }
            } else if (WSParams.schluessel == params[i]) {
                if (obj == null) {
                    result = new KksnTO(KksnFehler.EPAR, WSParams.schluessel, obj);
                    break;
                }
            } else if (WSParams.bilMonat == params[i]) {
                if (obj == null) {
                    result = new KksnTO(KksnFehler.EPAR, WSParams.bilMonat, obj);
                    break;
                }
            }
        }
        return result;
    }

    private void checkKlient(StKksnPersLesTO par) {
        if ((par != null) && ((par.getKlient() == null) || (par.getKlient().intValue() == 0))) {
            par.setKlient(defaultClient);
        }
    }
}

package de.deltalloyd.partnerdb.webservices.impl;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.deltalloyd.foundation.exception.AbstractCheckedSystemException;
import de.deltalloyd.partnerdb.core.service.AxaMaklerService;
import de.deltalloyd.partnerdb.orb.impl.OrbServiceEJB;
import de.deltalloyd.partnerdb.security.profiles.PdbBerechtigungen;
import de.deltalloyd.partnerdb.security.services.SecurityTokenSevice;
import de.deltalloyd.partnerdb.webservices.exception.PDBException;
import de.deltalloyd.partnerdb.webservices.exception.PartnerWebServiceException;
import de.deltalloyd.partnerdb.webservices.utils.PDBReleaseInfo;

@Stateless
@WebService(name = "AxaMaklerWebService", serviceName = "AxaMaklerWebService")
public class AxaMaklerWebService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AxaMaklerWebService.class);

	@PersistenceContext(unitName = "PDBPOOL")
	protected EntityManager emPDBPool;

	@EJB
	private OrbServiceEJB orbServiceEJB;

	@EJB
	private AxaMaklerService axaMaklerService;

	@EJB
	private SecurityTokenSevice sts;

	@WebMethod
	public List<Long> findAxaPartnerByExterneReferenz(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "externeReferenz") String externeReferenz) throws PartnerWebServiceException {
		try {
			sts.isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
		} catch (AbstractCheckedSystemException e) {
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
		return axaMaklerService.findAxaPartnerByExterneReferenz(externeReferenz);
	}

	@WebMethod
	public Boolean checkIfADMAOrbFuerAxaMaklerExists(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "admaSchluessel") String admaSchluessel) throws PartnerWebServiceException {
		try {
			sts.isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
		} catch (AbstractCheckedSystemException e) {
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
		return axaMaklerService.checkIfADMAOrbFuerAxaMaklerExists(admaSchluessel);
	}

	@WebMethod
	public Boolean erzeugeADMAOrbFuerAxaMakler(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "partnerNr") Long partnerNr) throws PartnerWebServiceException {
		boolean result = false;

		try {
			sts.isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);

			LOGGER.debug("erzeugeADMAOrbFuerAxaMakler with partnerNr: {}", partnerNr);
			result = axaMaklerService.erzeugeADMAOrbFuerAxaMakler(partnerNr);

		} catch (AbstractCheckedSystemException e) {
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}

		return result;
	}

	@WebMethod
	public String getReleaseInfo() {
		return PDBReleaseInfo.getReleaseInfo();
	}

}

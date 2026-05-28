package de.deltalloyd.partnerdb.rest.restWrapper;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import de.deltalloyd.partnerdb.webservices.exception.PartnerWebServiceException;
import de.deltalloyd.partnerdb.webservices.impl.AxaMaklerWebServiceDelegate;

@Path("/api/axamakler")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class AxaMaklerRestWrapper {

    @EJB
    private AxaMaklerWebServiceDelegate axaMaklerService;

    @GET
    @Path("/findByExterneReferenz")
    public List<Long> findAxaPartnerByExterneReferenz(
            @QueryParam("authToken") String authToken,
            @QueryParam("externeReferenz") String externeReferenz) throws PartnerWebServiceException {
        return axaMaklerService.findAxaPartnerByExterneReferenz(authToken, externeReferenz);
    }

    @GET
    @Path("/checkADMAOrb")
    public Boolean checkIfADMAOrbFuerAxaMaklerExists(
            @QueryParam("authToken") String authToken,
            @QueryParam("admaSchluessel") String admaSchluessel) throws PartnerWebServiceException {
        return axaMaklerService.checkIfADMAOrbFuerAxaMaklerExists(authToken, admaSchluessel);
    }

    @POST
    @Path("/erzeugeADMAOrb")
    public Boolean erzeugeADMAOrbFuerAxaMakler(
            @QueryParam("authToken") String authToken,
            @QueryParam("partnerNr") Long partnerNr) throws PartnerWebServiceException {
        return axaMaklerService.erzeugeADMAOrbFuerAxaMakler(authToken, partnerNr);
    }

    @GET
    @Path("/releaseInfo")
    public String getReleaseInfo() {
        return axaMaklerService.getReleaseInfo();
    }
}

package de.deltalloyd.partnerdb.rest.restWrapper;

import de.deltalloyd.partnerdb.orb.service.type.OrdnungsbegriffServiceTO;
import de.deltalloyd.partnerdb.orb.type.OrdnungsbegriffTO;
import de.deltalloyd.partnerdb.webservices.impl.OrbWebServiceImpl;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Path("/orb")
@Produces(MediaType.APPLICATION_XML)
@Consumes(MediaType.APPLICATION_XML)
public class OrbRestWrapper {

    @Inject
    OrbWebServiceImpl orbWebService;

    /**
     * Search for an Ordnungsbegriff.
     *
     * POST /orb/search
     */
    @POST
    @Path("/search")
    public OrdnungsbegriffTO sucheOrdnungsbegriffService(SucheOrdnungsbegriffDto dto) throws Exception {
        return orbWebService.sucheOrdnungsbegriffService(
                dto.getAuthToken(),
                dto.getOrbArtSchluessel(),
                dto.getFunkArtSchluessel(),
                dto.getaMandant(),
                dto.getaParameter());
    }

    /**
     * Search for an Ordnungsbegriff (full version).
     * Note: marked for deletion in release 13.0 per original code comment.
     *
     * POST /orb/search/full
     */
    @POST
    @Path("/search/full")
    public OrdnungsbegriffServiceTO sucheOrdnungsbegriffServiceFull(SucheOrdnungsbegriffDto dto) throws Exception {
        return orbWebService.sucheOrdnungsbegriffServiceFull(
                dto.getAuthToken(),
                dto.getOrbArtSchluessel(),
                dto.getFunkArtSchluessel(),
                dto.getaMandant(),
                dto.getaParameter());
    }

    /**
     * Get release info.
     *
     * POST /orb/system/release
     */
    @POST
    @Path("/system/release")
    public String getReleaseInfo() {
        return orbWebService.getReleaseInfo();
    }
}

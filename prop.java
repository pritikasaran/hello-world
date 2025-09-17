Properties prop = new Properties();
try (InputStream in = getClass().getClassLoader().getResourceAsStream("companion-config.properties")) {
    prop.load(in);
    pdb_wsdl = prop.getProperty("PDB_WSDL");
    companion_url = prop.getProperty("COMPANION_URL");
} catch (IOException e) {
    LOG.fatal("Fehler beim Laden der Konfiguration!", e);
    return false;
}

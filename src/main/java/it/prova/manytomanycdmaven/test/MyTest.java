package it.prova.manytomanycdmaven.test;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import it.prova.manytomanycdmaven.dao.EntityManagerUtil;
import it.prova.manytomanycdmaven.model.Cd;
import it.prova.manytomanycdmaven.model.Genere;
import it.prova.manytomanycdmaven.service.CdService;
import it.prova.manytomanycdmaven.service.GenereService;
import it.prova.manytomanycdmaven.service.MyServiceFactory;

public class MyTest {

	public static void main(String[] args) {
		CdService cdServiceInstance = MyServiceFactory.getCdServiceInstance();
		GenereService genereServiceInstance = MyServiceFactory.getGenereServiceInstance();

		try {

			System.out.println("In tabella Genere ci sono " + genereServiceInstance.listAll().size() + " elementi.");
			System.out.println("In tabella Cd ci sono " + cdServiceInstance.listAll().size() + " elementi.");
			System.out.println(
					"**************************** inizio batteria di test ********************************************");
			System.out.println(
					"*************************************************************************************************");

			testInserimentoNuovoCd(cdServiceInstance);

			testModificaECheckDateCd(cdServiceInstance);

			testInserimentoNuovoGenereERicercaPerDescrizione(genereServiceInstance);

			testCollegaGenereACd(cdServiceInstance, genereServiceInstance);

			testCreazioneECollegamentoCdInUnSoloColpo(cdServiceInstance, genereServiceInstance);

			testEstraiListaDescrizioneGeneriAssociateAdUnCd(cdServiceInstance, genereServiceInstance);

			// *********************************************************************************
			// RIMUOVIAMO UN CD E VEDIAMO COSA ACCADE AI GENERI
			// ********************************
			// per eseguire questo test dobbiamo prendere un cd esistente collegato a due
			// generi
			// il risultato atteso è la rimozione dalla tabella cd, la rimozione dalla
			// tabella
			// di legame lasciando inalterate le voci nella tabella genere. Tutto ciò
			// a prescindere della presenza dei Cascade. Se mettiamo CascadeType.ALL o
			// REMOVE...
			// DISASTRO!!!
			// *********************************************************************************
			testRimozioneCdECheckGeneri(cdServiceInstance, genereServiceInstance);

			testInserisciCDPartendoDaGenere(cdServiceInstance, genereServiceInstance);

			testCercaGeneriConCDTraDueDate(genereServiceInstance);

			testCercaCDConGeneriLungaDescrizione(cdServiceInstance);

			System.out.println(
					"****************************** fine batteria di test ********************************************");
			System.out.println(
					"*************************************************************************************************");
			System.out.println("In tabella Genere ci sono " + genereServiceInstance.listAll().size() + " elementi.");
			System.out.println("In tabella Cd ci sono " + cdServiceInstance.listAll().size() + " elementi.");

		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			// questa è necessaria per chiudere tutte le connessioni quindi rilasciare il
			// main
			EntityManagerUtil.shutdown();
		}

	}

	private static void testInserimentoNuovoCd(CdService cdServiceInstance) throws Exception {
		System.out.println(".......testInserimentoNuovoCd inizio.............");

		Cd cdInstance = new Cd("titolo1", "autore1", new SimpleDateFormat("dd/MM/yyyy").parse("24/09/2019"));
		cdServiceInstance.inserisciNuovo(cdInstance);
		if (cdInstance.getId() == null)
			throw new RuntimeException("testInserimentoNuovoCd fallito ");

		System.out.println(".......testInserimentoNuovoCd fine: PASSED.............");
	}

	private static void testModificaECheckDateCd(CdService cdServiceInstance) throws Exception {
		System.out.println(".......testModificaECheckDateCd inizio.............");

		Cd cdInstance = new Cd("titolo23", "autore211", new SimpleDateFormat("dd/MM/yyyy").parse("24/09/2021"));
		cdServiceInstance.inserisciNuovo(cdInstance);
		if (cdInstance.getId() == null)
			throw new RuntimeException("testModificaECheckDateCd fallito ");

		// ora mi salvo da parte le date di creazione ed update
		LocalDateTime createDateTimeIniziale = cdInstance.getCreateDateTime();
		LocalDateTime updateDateTimeIniziale = cdInstance.getUpdateDateTime();

		// **************************************************************************************************
		// **************************************************************************************************
		// all'inizio DOVREBBERO essere uguali, infatti a volte non lo sono per
		// questione di 10^-4 secondi
		// soluzione: riprovare!!! Se diventa sistematico commentare le due righe
		// successive
		if (!createDateTimeIniziale.equals(updateDateTimeIniziale))
			throw new RuntimeException("testModificaECheckDateCd fallito: le date non coincidono ");
		// **************************************************************************************************
		// **************************************************************************************************

		// ora modifico il record
		cdInstance.setAutore("Mio nuovo autore");
		cdServiceInstance.aggiorna(cdInstance);

		// se la nuova data aggiornamento risulta precedente a quella iniziale: errore
		if (cdInstance.getUpdateDateTime().isAfter(updateDateTimeIniziale))
			throw new RuntimeException("testModificaECheckDateCd fallito: le date di modifica sono disallineate ");

		// la data creazione deve essere uguale a quella di prima
		if (!cdInstance.getCreateDateTime().equals(createDateTimeIniziale))
			throw new RuntimeException("testModificaECheckDateCd fallito: la data di creazione è cambiata ");

		System.out.println(".......testModificaECheckDateCd fine: PASSED.............");
	}

	private static void testInserimentoNuovoGenereERicercaPerDescrizione(GenereService genereServiceInstance)
			throws Exception {
		System.out.println(".......testInserimentoNuovoGenereERicercaPerDescrizione inizio.............");

		// creo una cosa del tipo rock1634630578974 così ogni volta sarà diverso
		String descrizioneGenere = "rock" + new Date().getTime();
		Genere nuovoGenere = new Genere(descrizioneGenere);
		genereServiceInstance.inserisciNuovo(nuovoGenere);
		if (nuovoGenere.getId() == null)
			throw new RuntimeException(
					"testInserimentoNuovoGenereERicercaPerDescrizione fallito: genere non inserito ");

		if (genereServiceInstance.cercaPerDescrizione(descrizioneGenere) == null)
			throw new RuntimeException(
					"testInserimentoNuovoGenereERicercaPerDescrizione fallito: cercaPerDescrizione di genere non ha ritrovato elementi ");

		System.out.println(".......testInserimentoNuovoGenereERicercaPerDescrizione fine: PASSED.............");
	}

	private static void testCollegaGenereACd(CdService cdServiceInstance, GenereService genereServiceInstance)
			throws Exception {
		System.out.println(".......testCollegaGenereACd inizio.............");

		long nowInMillisecondi = new Date().getTime();
		// inserisco un cd
		Cd cdInstance = new Cd("titolo" + nowInMillisecondi, "autore" + nowInMillisecondi,
				new SimpleDateFormat("dd/MM/yyyy").parse("12/01/2020"));
		cdServiceInstance.inserisciNuovo(cdInstance);
		if (cdInstance.getId() == null)
			throw new RuntimeException("testCollegaGenereACd fallito: inserimento cd non riuscito ");

		// inserisco un genere
		Genere nuovoGenere = new Genere("rock" + nowInMillisecondi);
		genereServiceInstance.inserisciNuovo(nuovoGenere);
		if (nuovoGenere.getId() == null)
			throw new RuntimeException("testCollegaGenereACd fallito: genere non inserito ");

		// collego
		cdServiceInstance.aggiungiGenere(cdInstance, nuovoGenere);

		// ricarico eager per forzare il test
		Cd cdReloaded = cdServiceInstance.caricaSingoloElementoEagerGeneri(cdInstance.getId());
		if (cdReloaded.getGeneri().isEmpty())
			throw new RuntimeException("testCollegaGenereACd fallito: genere non collegato ");

		System.out.println(".......testCollegaGenereACd fine: PASSED.............");
	}

	private static void testCreazioneECollegamentoCdInUnSoloColpo(CdService cdServiceInstance,
			GenereService genereServiceInstance) throws Exception {
		System.out.println(".......testCreazioneECollegamentoCdInUnSoloColpo inizio.............");

		long nowInMillisecondi = new Date().getTime();
		Cd cdInstanceX = new Cd("titolo" + nowInMillisecondi, "autore" + nowInMillisecondi,
				new SimpleDateFormat("dd/MM/yyyy").parse("10/08/2020"));
		Genere genereX = new Genere("genere" + nowInMillisecondi);
		cdServiceInstance.creaECollegaCdEGenere(cdInstanceX, genereX);

		if (cdInstanceX.getId() == null)
			throw new RuntimeException("testCreazioneECollegamentoCdInUnSoloColpo fallito: cd non inserito ");

		if (genereX.getId() == null)
			throw new RuntimeException("testCreazioneECollegamentoCdInUnSoloColpo fallito: genere non inserito ");

		// ricarico eager per forzare il test
		Cd cdReloaded = cdServiceInstance.caricaSingoloElementoEagerGeneri(cdInstanceX.getId());
		if (cdReloaded.getGeneri().isEmpty())
			throw new RuntimeException("testCreazioneECollegamentoCdInUnSoloColpo fallito: genere e cd non collegati ");

		System.out.println(".......testCreazioneECollegamentoCdInUnSoloColpo fine: PASSED.............");
	}

	private static void testRimozioneCdECheckGeneri(CdService cdServiceInstance, GenereService genereServiceInstance)
			throws Exception {
		System.out.println(".......testRimozioneCdECheckGeneri inizio.............");

		// creo un cd e due generi
		long nowInMillisecondi = new Date().getTime();
		Cd cdInstanceX = new Cd("titolo" + nowInMillisecondi, "autore" + nowInMillisecondi,
				new SimpleDateFormat("dd/MM/yyyy").parse("10/08/2020"));
		cdServiceInstance.inserisciNuovo(cdInstanceX);
		Genere genere1 = new Genere("genere" + nowInMillisecondi);
		genereServiceInstance.inserisciNuovo(genere1);
		Genere genere2 = new Genere("genere" + nowInMillisecondi + 1);
		genereServiceInstance.inserisciNuovo(genere2);
		cdServiceInstance.aggiungiGenere(cdInstanceX, genere1);
		cdServiceInstance.aggiungiGenere(cdInstanceX, genere2);

		// ricarico eager per forzare il test
		Cd cdReloaded = cdServiceInstance.caricaSingoloElementoEagerGeneri(cdInstanceX.getId());
		if (cdReloaded.getGeneri().size() != 2)
			throw new RuntimeException("testRimozioneCdECheckGeneri fallito: 2 generi e cd non collegati ");

		// rimuovo
		cdServiceInstance.rimuovi(cdReloaded.getId());

		// ricarico
		Cd cdSupposedToBeRemoved = cdServiceInstance.caricaSingoloElementoEagerGeneri(cdInstanceX.getId());
		if (cdSupposedToBeRemoved != null)
			throw new RuntimeException("testRimozioneCdECheckGeneri fallito: rimozione non avvenuta ");

		System.out.println(".......testRimozioneCdECheckGeneri fine: PASSED.............");
	}

	private static void testEstraiListaDescrizioneGeneriAssociateAdUnCd(CdService cdServiceInstance,
			GenereService genereServiceInstance) throws Exception {
		System.out.println(".......testEstraiListaDescrizioneGeneriAssociateAdUnCd inizio.............");

		// creo un cd e tre generi
		long nowInMillisecondi = new Date().getTime();
		Cd cdInstanceX = new Cd("titolo" + nowInMillisecondi, "autore" + nowInMillisecondi,
				new SimpleDateFormat("dd/MM/yyyy").parse("10/08/2020"));
		cdServiceInstance.inserisciNuovo(cdInstanceX);
		Genere genere1 = new Genere("genere" + nowInMillisecondi);
		genereServiceInstance.inserisciNuovo(genere1);
		Genere genere2 = new Genere("genere" + nowInMillisecondi + 1);
		genereServiceInstance.inserisciNuovo(genere2);
		Genere genere3 = new Genere("genere" + nowInMillisecondi + 2);
		genereServiceInstance.inserisciNuovo(genere3);
		cdServiceInstance.aggiungiGenere(cdInstanceX, genere1);
		cdServiceInstance.aggiungiGenere(cdInstanceX, genere2);
		cdServiceInstance.aggiungiGenere(cdInstanceX, genere3);

		// ricarico eager per forzare il test
		Cd cdReloaded = cdServiceInstance.caricaSingoloElementoEagerGeneri(cdInstanceX.getId());
		if (cdReloaded.getGeneri().size() != 3)
			throw new RuntimeException(
					"testEstraiListaDescrizioneGeneriAssociateAdUnCd fallito: 2 generi e cd non collegati ");

		// vediamo se estrae 3 descrizioni
		List<String> listaGeneriAssociatiAlCdReloaded = cdServiceInstance
				.estraiListaDescrizioneGeneriAssociateAdUnCd(cdReloaded.getId());
		if (listaGeneriAssociatiAlCdReloaded == null || listaGeneriAssociatiAlCdReloaded.isEmpty()
				|| listaGeneriAssociatiAlCdReloaded.size() != 3)
			throw new RuntimeException(
					"testEstraiListaDescrizioneGeneriAssociateAdUnCd fallito: nessuna descrizione caricata ");

		// adesso un test più stringente
		for (String descrizioneItem : Arrays.asList(genere1.getDescrizione(), genere2.getDescrizione(),
				genere3.getDescrizione())) {
			if (!listaGeneriAssociatiAlCdReloaded.contains(descrizioneItem))
				throw new RuntimeException("testEstraiListaDescrizioneGeneriAssociateAdUnCd fallito: descrizione "
						+ descrizioneItem + " non contenuta nella lista estratta");
		}

		System.out.println(".......testEstraiListaDescrizioneGeneriAssociateAdUnCd fine: PASSED.............");
	}

	private static void testInserisciCDPartendoDaGenere(CdService cdServiceInstance,
			GenereService genereServiceInstance) throws Exception {
		System.out.println("testInserisciCDPartendoDaGenere inizializzato........");

		long nowInMillisecondi = new Date().getTime();

		Genere genereTestInserimento = new Genere("metal" + nowInMillisecondi);
		genereServiceInstance.inserisciNuovo(genereTestInserimento);
		if (genereTestInserimento.getId() == null)
			throw new RuntimeException("testCollegaGenereACd fallito: genere non inserito ");

		Cd cdTestInserimento = new Cd("Una notte" + nowInMillisecondi, "Nuovo Autore" + nowInMillisecondi,
				new SimpleDateFormat("dd/MM/yyyy").parse("12/01/2020"));
		cdServiceInstance.inserisciNuovo(cdTestInserimento);
		if (cdTestInserimento.getId() == null)
			throw new RuntimeException("testCollegaGenereACd fallito: inserimento cd non riuscito ");

		genereServiceInstance.aggiungiCd(genereTestInserimento, cdTestInserimento);

		Genere genereRicaricato = genereServiceInstance.caricaSingoloElementoEager(cdTestInserimento.getId());

		System.out.println("testInserisciCDPartendoDaGenere concluso........");
	}

	private static void testCercaGeneriConCDTraDueDate(GenereService genereService) throws Exception {
		System.out.println("testCercaGeneriConCDTraDueDate inizializzato......");

		Date dataNumero1 = new SimpleDateFormat("yyyy-MM-dd").parse("2019-01-01");
		Date dataNumero2 = new SimpleDateFormat("yyyy-MM-dd").parse("2020-12-31");

		List<Genere> listaGeneri = genereService.cercaTuttiGeneriConCDPubblicatiTra(dataNumero1, dataNumero2);
		for (Genere genereInput : listaGeneri)
			System.out.println(genereInput);

		System.out.println("testCercaGeneriConCDTraDueDate concluso......");
	}

	private static void testCercaCDConGeneriLungaDescrizione(CdService cdServiceInstance) throws Exception {
		System.out.println("testCercaCDConGeneriLungaDescrizione inizializzato.......");

		List<Cd> risultatoQuery = cdServiceInstance.cercaTuttiCDConGeneriDallaDescrizioneLunga();

		for (Cd cdInput : risultatoQuery)
			System.out.println(cdInput);

		System.out.println("testCercaCDConGeneriLungaDescrizione concluso.......");
	}

}

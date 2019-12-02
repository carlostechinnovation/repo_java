/**
 * 
 */
package fotos.duplicados;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.commons.io.FileUtils;

/**
 * Dada una carpeta, busca y elimina todos los duplicados en esa carpeta y
 * subcarpetas. Dada una foto original, si encuentra el duplicado, lo borra.
 *
 */
public class LimpiarDuplicados {

	public static final String PATH_BASE = "/MIRUTAPADRE";
	public static final String DIR_IMG = PATH_BASE+"/img";
	public static final String DIR_VID = PATH_BASE+"/vid";

	private static boolean isFinished = false;

	public static void main(String[] args) throws IOException {

		// Path + hash + tamanho --> Ante dos hash iguales, pero con distinto tamanho,
		// lanza warning

		List<String> listaFicheros = new ArrayList<String>();

		File file = new File(PATH_BASE);
		fetchFiles(file, f -> listaFicheros.add(f.getAbsolutePath()));

		System.out.println("Rutas sacadas: " + listaFicheros.size());

		// -------------------------------
		Map<String, String> mapa = new HashMap<String, String>();
		Long contador = 0L;

		for (String path_file : listaFicheros) {
			contador++;
			if (contador % 100 == 0) {
				System.out.println(contador + "...");
			}

			long cs = getChecksumValue(new CRC32(), path_file);
			long tamanioBytes = FileUtils.sizeOf(new File(path_file));

			mapa.put(path_file, new String(cs + "|" + tamanioBytes)); // CHECKSUM + TAMANHO
		}

		List<String> candidatoDuplicados = buscaDuplicados(mapa);

		System.out.println("\nDuplicados encontrados (que queremos borrar) = " + candidatoDuplicados.size());

		// --------- OPERACIONES -------------
		eliminarListaFicheros(candidatoDuplicados);
		acumularEnCarpetaUnica(listaFicheros, PATH_BASE);
		limpiarCarpetasVacias(PATH_BASE);
		clasificarPorTipo(PATH_BASE);

	}

	/**
	 * @param dir
	 * @param fileConsumer
	 */
	public static void fetchFiles(File dir, Consumer<File> fileConsumer) {

		// Directorios especiales que queremos excluir

		if (dir.isDirectory()) {
			for (File file1 : dir.listFiles()) {
				fetchFiles(file1, fileConsumer);
			}
		} else {
			fileConsumer.accept(dir);
		}

	}

	/**
	 * @param checksum
	 * @param fname
	 * @return
	 */
	public static long getChecksumValue(Checksum checksum, String fname) {
		try {
			BufferedInputStream is = new BufferedInputStream(new FileInputStream(fname));
			byte[] bytes = new byte[1024];
			int len = 0;

			while ((len = is.read(bytes)) >= 0) {
				checksum.update(bytes, 0, len);
			}
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return checksum.getValue();
	}

	/**
	 * Saca una lista de los duplicados (que son los que borrar�)
	 * 
	 * @param mapa
	 * @return
	 */
	public static List<String> buscaDuplicados(Map<String, String> mapa) {

		List<String> valoresLeidos = new ArrayList<String>();
		List<String> soloClavesDeDuplicados = new ArrayList<String>();

		System.out.println("Buscando duplicados en un map de " + mapa.size() + " claves...");

		for (String path_file : mapa.keySet()) {

			String valorAnalizado = mapa.get(path_file);

			if (valoresLeidos.contains(valorAnalizado)) {
				// VALOR Duplicado encontrado
				soloClavesDeDuplicados.add(path_file);

			} else {
				// System.out.println("Salvado --> " + path_file + " -> " + valorAnalizado);
				valoresLeidos.add(valorAnalizado);
			}
		}

		System.out.println("Buscando duplicados: FIN");
		return soloClavesDeDuplicados;
	}

	/**
	 * @param eliminables
	 */
	public static void eliminarListaFicheros(List<String> eliminables) {

		Long numEliminados = 0L;
		Long numErrores = 0L;

		for (String path_eliminar : eliminables) {
			File fileEliminar = new File(path_eliminar);
			if (fileEliminar.delete()) {
				System.out.println("Borrado --> " + path_eliminar);
				numEliminados++;
			} else {
				System.err.println("Error al borrar --> " + path_eliminar);
				numErrores++;
			}
		}

		System.out.println("Numero eliminados = " + numEliminados);
		System.out.println("Numero errores = " + numErrores);
	}

	/**
	 * @param listaFicheros
	 * @param dirDestinoPath
	 * @throws IOException
	 */
	public static void acumularEnCarpetaUnica(List<String> listaFicheros, String dirDestinoPath) throws IOException {

		System.out.println("Acumulando ficheros en carpeta unica: " + dirDestinoPath);
		long numero = 100000;
		int numFicherosMovidos = 0;

		for (String path_file : listaFicheros) {

			do {
				numero++;
			} while (Files.exists(Paths.get(dirDestinoPath + "/" + String.valueOf(numero)), LinkOption.NOFOLLOW_LINKS));

			if (Files.exists(Paths.get(path_file))) {
				Files.move(Paths.get(path_file), Paths.get(dirDestinoPath + "/" + String.valueOf(numero)));
				numFicherosMovidos++;
			}
		}

		System.out.println("Numero de ficheros movidos a carpeta unica: " + numFicherosMovidos);

	}

	/**
	 * @param dirPadre
	 * @throws IOException
	 */
	public static void limpiarCarpetasVacias(String dirPadre) throws IOException {

		System.out.println("Limpiando carpetas vacias dentro de esta carpeta padre: " + dirPadre);

		do {
			isFinished = true;
			limpiarCarpetasVaciasNucleo(dirPadre);
		} while (!isFinished);

	}

	/**
	 * @param dirPadre
	 * @throws IOException
	 */
	public static void limpiarCarpetasVaciasNucleo(String dirPadre) throws IOException {

		System.out.println("Limpiando carpetas vacias dentro de esta carpeta padre: " + dirPadre);

		File folder = new File(dirPadre);
		File[] listofFiles = folder.listFiles();
		if (listofFiles.length == 0) {
			System.out.println("Folder Name :: " + folder.getAbsolutePath() + " is deleted.");
			folder.delete();
			isFinished = false;
		} else {
			for (int j = 0; j < listofFiles.length; j++) {
				File file = listofFiles[j];
				if (file.isDirectory()) {
					limpiarCarpetasVaciasNucleo(file.getAbsolutePath());
				}
			}
		}

	}

	/**
	 * @param dirPadre
	 * @throws IOException
	 */
	public static void clasificarPorTipo(String dirPadre) throws IOException {

		System.out.println("Clasificando los ficheros por tipo, en subcarpetas: " + dirPadre);

		String pathDirImagenes = dirPadre + "/img/";
		String pathDirVideos = dirPadre + "/vid/";

		if (Files.notExists(Paths.get(pathDirImagenes))) {
			Files.createDirectory(Paths.get(pathDirImagenes));
		}

		if (Files.notExists(Paths.get(pathDirVideos))) {
			Files.createDirectory(Paths.get(pathDirVideos));
		}

		File file = new File(dirPadre);
		List<String> listaFicheros = new ArrayList<String>();
		fetchFiles(file, f -> listaFicheros.add(f.getAbsolutePath()));
		Long numeroFicheroRevisado = 1L;

		for (String path_file : listaFicheros) {
			
			if (numeroFicheroRevisado % 100 == 0) {
				System.out.println("Clasificado ficheros por tipos... (nos llegamos por "+numeroFicheroRevisado + "...");
			}
			
			String tipo = identifyFileTypeUsingFilesProbeContentType(path_file);

			if (tipo.contains("image")) {
				Files.move(Paths.get(path_file), Paths.get(DIR_IMG + "/" + Paths.get(path_file).getFileName()));
			} else if (tipo.contains("video")) {
				Files.move(Paths.get(path_file), Paths.get(DIR_VID + "/" + Paths.get(path_file).getFileName()));
			} else {
				System.out.println("TIPO DESCONOCIDO --> " + path_file + " -> " + tipo);
			}
			numeroFicheroRevisado++;
		}

	}

	/**
	 * Identify file type of file with provided path and name using JDK 7's
	 * Files.probeContentType(Path).
	 *
	 * @param fileName Name of file whose type is desired.
	 * @return String representing identified type of file with provided name.
	 */
	public static String identifyFileTypeUsingFilesProbeContentType(final String fileName) {

		String fileType = "desconocido";
		final File file = new File(fileName);
		try {
			fileType = Files.probeContentType(file.toPath());
		} catch (IOException ioException) {
			System.out.println(
					"ERROR: Unable to determine file type for " + fileName + " due to exception " + ioException);
		}
		return fileType;
	}
}

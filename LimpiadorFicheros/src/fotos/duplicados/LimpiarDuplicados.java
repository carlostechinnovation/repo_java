package fotos.duplicados;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.FileUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;

/**
 * Dada una carpeta, busca y elimina todos los duplicados en esa carpeta y
 * subcarpetas. Dada una foto original, si encuentra el duplicado, lo borra.
 *
 */
public class LimpiarDuplicados {

	public static final String PATH_BASE = "D:\\Imágenes\\NO_ANIMALES\\";
	public static final String DIR_IMG = PATH_BASE + "/img";
	public static final String DIR_VID = PATH_BASE + "/vid";

	private static boolean isFinished = false;
	private static SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	public static void main(String[] args) throws IOException, TikaException {

		// Path + hash + tamanho --> Ante dos hash iguales, pero con distinto tamanho,
		// lanza warning

		System.out.println(
				formatter.format(new Date()) + " - Obteniendo subcarpetas y ficheros de PATH_BASE=" + PATH_BASE);
		List<String> listaFicheros = new ArrayList<String>();
		File file = new File(PATH_BASE);
		fetchFiles(file, f -> listaFicheros.add(f.getAbsolutePath()), 1);

		System.out.println(formatter.format(new Date()) + " - Rutas sacadas: " + listaFicheros.size());

		// -------------------------------
		Map<String, String> mapa = new HashMap<String, String>();
		Long contador = 0L;

		for (String path_file : listaFicheros) {
			contador++;
			if (contador % 100 == 0) {
				System.out.println(formatter.format(new Date()) + " - " + contador + "...");
			}

			long cs = getChecksumValue(new CRC32(), path_file);
			long tamanioBytes = FileUtils.sizeOf(new File(path_file));

			mapa.put(path_file, new String(cs + "|" + tamanioBytes)); // CHECKSUM + TAMANHO
		}

		List<String> candidatoDuplicados = buscaDuplicados(mapa);

		System.out.println(formatter.format(new Date()) + " - Duplicados encontrados (que queremos borrar) = "
				+ candidatoDuplicados.size());

		// --------- OPERACIONES -------------
		eliminarListaFicheros(candidatoDuplicados);
//		acumularEnCarpetaUnica(listaFicheros, PATH_BASE);
//		limpiarCarpetasVacias(PATH_BASE);
//		clasificarPorTipo(PATH_BASE);
	}

	/**
	 * @param dir
	 * @param fileConsumer
	 * @param nivelProfundidad 1-superficial; 2-profundo.
	 */
	public static void fetchFiles(File dir, Consumer<File> fileConsumer, int nivelProfundidad) {

		// Directorios especiales que queremos excluir
		if (dir.isDirectory()) {

			if (nivelProfundidad == 1) {
				System.out.println(formatter.format(new Date()) + " Revisando directorio y sus subcarpetas: "
						+ dir.getAbsolutePath());
			}

			File[] lista = dir.listFiles();

			for (File file1 : lista) {
				if (nivelProfundidad == 1) {
					System.out.println(formatter.format(new Date()) + "\t Subcarpeta: " + file1.getAbsolutePath());
				}

				fetchFiles(file1, fileConsumer, 2);
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
	 * Saca una lista de los duplicados (que son los que borrara)
	 * 
	 * @param mapa
	 * @return
	 */
	public static List<String> buscaDuplicados(Map<String, String> mapa) {

		List<String> valoresLeidos = new ArrayList<String>();
		List<String> soloClavesDeDuplicados = new ArrayList<String>();

		System.out.println(
				formatter.format(new Date()) + " - Buscando duplicados en un map de " + mapa.size() + " claves...");

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

		System.out.println(formatter.format(new Date()) + " - Buscando duplicados: FIN");
		return soloClavesDeDuplicados;
	}

	/**
	 * @param eliminables
	 */
	public static void eliminarListaFicheros(List<String> eliminables) {
		System.out.println("eliminarListaFicheros() - INICIO");
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
		System.out.println("eliminarListaFicheros() - FIN");
	}

	/**
	 * @param listaFicheros
	 * @param dirDestinoPath
	 * @throws IOException
	 * @throws TikaException
	 */
	public static void acumularEnCarpetaUnica(List<String> listaFicheros, String dirDestinoPath)
			throws IOException, TikaException {

		System.out.println("acumularEnCarpetaUnica() - INICIO");

		System.out.println("Acumulando ficheros en carpeta unica: " + dirDestinoPath);
		long numero = 100000;
		int numFicherosMovidos = 0;

		for (String path_file : listaFicheros) {

			Path nombreFicheroDestino = Paths.get(crearPathCompletoDestino(path_file, dirDestinoPath, numero));
			while (Files.exists(nombreFicheroDestino, LinkOption.NOFOLLOW_LINKS)) {
				numero++;
				nombreFicheroDestino = Paths.get(crearPathCompletoDestino(path_file, dirDestinoPath, numero));
			}

			if (Files.exists(Paths.get(path_file))) {
				Files.move(Paths.get(path_file), nombreFicheroDestino, StandardCopyOption.REPLACE_EXISTING);
				numFicherosMovidos++;
			}
		}

		System.out.println("Numero de ficheros movidos a carpeta unica: " + numFicherosMovidos);
		System.out.println("acumularEnCarpetaUnica() - FIN");
	}

	/**
	 * @param path_file_origen
	 * @param dirDestinoPath
	 * @param numero
	 * @return
	 * @throws TikaException
	 * @throws IOException
	 */
	public static String crearPathCompletoDestino(String path_file_origen, String dirDestinoPath, long numero)
			throws TikaException, IOException {

		System.out.println("path_file_origen=" + path_file_origen + " --> dirDestinoPath=" + dirDestinoPath
				+ " -->numero=" + numero);

		long tamanioBytes = FileUtils.sizeOf(new File(path_file_origen));

		MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
		Object tipo = fileTypeMap.getContentType(path_file_origen);
		TikaConfig tikaConfig = new TikaConfig();
		Detector detector = tikaConfig.getDetector();
		File fichero = new File(path_file_origen);
		InputStream is = new FileInputStream(fichero);
		TikaInputStream stream = TikaInputStream.get(is);
		Metadata metadata = new Metadata();
		metadata.add(Metadata.RESOURCE_NAME_KEY, fichero.getName());
		MediaType mediaType = detector.detect(stream, metadata);
		MimeType mimeType = tikaConfig.getMimeRepository().forName(mediaType.toString());
		String mimeExtension = mimeType.getExtension();

		System.out.println("mimeExtension=" + mimeExtension);
		String extension = "";
		if (mimeExtension != null && !mimeExtension.isEmpty()) {
			extension = mimeExtension.split("\\.")[1];
		} else {
			// SI ESTA VACIO
			extension = (tamanioBytes < 5000000) ? "png" : "mp4"; // Si ocupa <5MB es PNG; si no, es MP4
		}

		// ESPECIALES
		if (!extension.isEmpty() && (extension.equals("qt") || extension.equals("bin"))) {
			extension = "mp4";
		}

		String pathSalida = dirDestinoPath + "/" + String.valueOf(numero) + "." + extension;
		System.out.println("path_file_origen=" + path_file_origen + "------>SIZE (Bytes)=" + tamanioBytes
				+ "------>TIPO=" + tipo + "------>Extension=" + extension + " =====> " + pathSalida);

		return pathSalida;
	}

	/**
	 * @param dirPadre
	 * @throws IOException
	 */
	public static void limpiarCarpetasVacias(String dirPadre) throws IOException {

		System.out.println("limpiarCarpetasVacias() - INICIO");
		System.out.println("Limpiando carpetas vacias dentro de esta carpeta padre: " + dirPadre);

		do {
			isFinished = true;
			limpiarCarpetasVaciasNucleo(dirPadre);
		} while (!isFinished);

		System.out.println("limpiarCarpetasVacias() - FIN");
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

		System.out.println("clasificarPorTipo() - INICIO");
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
		fetchFiles(file, f -> listaFicheros.add(f.getAbsolutePath()), 1);
		Long numeroFicheroRevisado = 1L;

		for (String path_file : listaFicheros) {

			if (numeroFicheroRevisado % 100 == 0) {
				System.out
						.println("Clasificado ficheros por tipos... (nos llegamos por " + numeroFicheroRevisado + ")");
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

		System.out.println("clasificarPorTipo() - FIN");
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

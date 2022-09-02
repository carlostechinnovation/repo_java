package fotos.clasificador;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

/**
 * Dada una carpeta de ficheros PENDIENTES, analiza uno por uno todos los
 * ficheros. Interpreta el nombre de cada fichero y lo recoloca en otra carpeta
 * llamada PENDIENTES_AAAA (donde AAAA es el anio adecuado). Si no es capaz de
 * saber de qué año es, no lo recoloca y muestra un warning. Si al recolocarlo
 * ya existe un fichero idéntico (hash y tamaño; no nombre), lo considera ya
 * movido y borra el fichero origen.
 */
public class Clasificador {

	public static final String DIR_ORIGEN = "D:\\Imágenes\\NO_ANIMALES\\PENDIENTES\\";
	public static final String DIR_DESTINO_PREFIJO_AAAA = "D:\\Imágenes\\NO_ANIMALES\\PENDIENTE_";
	public static final String DIR_BORRADO_LOGICO = "D:\\Imágenes\\NO_ANIMALES\\PENDIENTE_BORRADO_LOGICO\\";

	public static final Integer ANIO_INI = 2008;
	public static final Integer ANIO_FIN = 2022;

	private static int LOG_INCREMENTO = 100;
	public static SimpleDateFormat formato_YYYYMMDD_HHMMSS = new SimpleDateFormat("yyyyMMdd_hhmmss");
	public static SimpleDateFormat formato_YYYYMMDD = new SimpleDateFormat("yyyyMMdd");

	public static void main(String[] args) throws IOException, TikaException, ImageProcessingException {

		// ------- LISTA DE FICHEROS DESTINO (preexistentes)------------------------
		Map<String, String> mapaDestino = revisarCarpetasDestinoAnualizadas(DIR_DESTINO_PREFIJO_AAAA, ANIO_INI,
				ANIO_FIN);

		// ------- MAPA DE FICHEROS ORIGEN------------------------
		List<String> listaFicherosOrigen = new ArrayList<String>();

		File file = new File(DIR_ORIGEN);
		fetchFiles(file, f -> listaFicherosOrigen.add(f.getAbsolutePath()));
		System.out.println("Ficheros origen - Rutas: " + listaFicherosOrigen.size());

		List<String> pathProcesadosBien = new ArrayList<String>();
		List<String> pathProcesadosMal = new ArrayList<String>();
		Long contador = 0L;

		for (String path_file_origen : listaFicherosOrigen) {
			contador++;
			if (contador % LOG_INCREMENTO == 0) {
				System.out.println(contador + "...");
			}

			Integer anioDeducido = deducirAniodeFicheroOrigen(path_file_origen, ANIO_INI, ANIO_FIN, false);

			// 1. Si el año deducido es NULL, no se hace nada con este fichero. No se toca,
			// pero se muestra un warning
			if (anioDeducido == null) {
				System.err.println(
						"No se puede deducir el ANIO del siguiente fichero origen. No se hace nada con el fichero. Path:"
								+ path_file_origen);
				pathProcesadosMal.add(path_file_origen);

			} else {

				String claveOrigen = calcularClave(anioDeducido, path_file_origen);
				String dirDestino = DIR_DESTINO_PREFIJO_AAAA + anioDeducido + "\\";

				// 2.Ya existe la CLAVE en destino: lo borro del origen (borrado lógico)
				if (mapaDestino.containsKey(claveOrigen)) {

					if (Files.notExists(Paths.get(DIR_BORRADO_LOGICO))) {
						Files.createDirectory(Paths.get(DIR_BORRADO_LOGICO));
					}

					String sufijoRandom = String.valueOf((new java.util.Random()).nextInt());
					Path path_file_borrado_con_sufijo_random = Paths
							.get(DIR_BORRADO_LOGICO + Paths.get(path_file_origen).getFileName() + "_" + sufijoRandom);

					Files.move(Paths.get(path_file_origen), path_file_borrado_con_sufijo_random);
					pathProcesadosBien.add(path_file_origen);
					System.out.println("Escenario 2 - Ya existe la CLAVE en destino (" + anioDeducido
							+ "): lo borro del origen (borrado lógico)");

				} else {

					Path path_file_destino = Paths.get(dirDestino + Paths.get(path_file_origen).getFileName());

					if (Files.exists(path_file_destino)) {

						// 3. Ya existe el NOMBRE en destino, se añade un SUFIJO RANDOM y se mueve
						String sufijoRandom = String.valueOf((new java.util.Random()).nextInt());
						Path path_file_destino_con_sufijo_random = Paths
								.get(dirDestino + Paths.get(path_file_origen).getFileName() + "_" + sufijoRandom);

						Files.move(Paths.get(path_file_origen), path_file_destino_con_sufijo_random);
						pathProcesadosBien.add(path_file_origen);
						System.out.println("Escenario 3 - Ya existe el NOMBRE en destino (" + anioDeducido
								+ "), se añade un SUFIJO RANDOM y se mueve");

					} else {
						// 4. HABITUAL: no existe clave ni nombre en destino, asi que se mueve
						Files.move(Paths.get(path_file_origen), path_file_destino);
						pathProcesadosBien.add(path_file_origen);
						System.out.println("Escenario 4 - HABITUAL: no existe clave ni nombre en destino ("
								+ anioDeducido + "), asi que se mueve");
					}
				}
			}

		}

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
	 * Mirando el nombre del fichero y sus propiedades, intenta deducir el año en el
	 * que se debe clasificar.
	 * 
	 * @param pathFicheroOrigen Path del fichero analizado
	 * @param rangoInicio
	 * @param rangoFin
	 * @param modoDebug
	 * @return Anio deducido. Si no se consigue, devuelve NULL.
	 * @throws IOException
	 * @throws ImageProcessingException
	 */
	public static Integer deducirAniodeFicheroOrigen(String pathFicheroOrigen, Integer rangoInicio, Integer rangoFin,
			boolean modoDebug) throws IOException, ImageProcessingException {

		System.out.println("Deduciendo fecha de: " + pathFicheroOrigen);

		Integer out = null; // default

		String filename = Paths.get(pathFicheroOrigen).getFileName().toString();
		String limpio = filename;

		// Quitar prefijos conocidos
		if (limpio.contains("FB_")) {
			limpio = limpio.replace("FB_", "");
		}
		if (limpio.contains("IMG")) {
			limpio = limpio.replace("IMG", "");
		}
		if (limpio.contains("VID")) {
			limpio = limpio.replace("VID", "");
		}
		if (limpio.contains("Screenshot_")) {
			limpio = limpio.replace("Screenshot_", "");
		}
		if (limpio.contains("_")) {
			limpio = limpio.replace("_", "");
		}
		if (limpio.contains("-")) {
			limpio = limpio.replace("-", "");
		}

		// Quitar extension
		if (limpio.contains(".")) {
			limpio = limpio.substring(0, limpio.lastIndexOf('.'));
		}

		// Empieza por patron YYYYMMDD_HHMMSS
		if (limpio.length() >= (1 + 8 + 1 + 6) && checkFormatoYYYYMMDDHHMMSS(limpio.substring(0, 8 + 6))) {
			out = Integer.valueOf(limpio.substring(0, 4));
		}

		// Empieza por patron YYYYMMDD_
		else if (limpio.length() >= (1 + 8) && checkFormatoYYYYMMDD(limpio.substring(0, 8))) {
			out = Integer.valueOf(limpio.substring(0, 4));
		}

		// PROPIEDADES INTERNAS (Fecha de captura...)
		else {
			out = procesarPropiedadesYextraerAnio(pathFicheroOrigen, modoDebug);
		}

		// Comprobar que esta dentro del rango
		if (out != null && (out >= rangoInicio && out <= rangoFin) == false) {
			out = null; // invalida el dato deducido fuera de rango
		}

		return out;
	}

	/**
	 * @param pathFicheroOrigen
	 * @param modoDebug
	 * @return
	 */
	public static Integer procesarPropiedadesYextraerAnio(String pathFicheroOrigen, boolean modoDebug) {

		Integer out = null; // default

		File fichero = new File(pathFicheroOrigen);
//		BasicFileAttributes attrs = Files.readAttributes(fichero.toPath(), BasicFileAttributes.class);
//		System.out.println("creationTime: " + attrs.creationTime());
//		System.out.println("lastAccessTime: " + attrs.lastAccessTime());
//		System.out.println("lastModifiedTime: " + attrs.lastModifiedTime());

		// https://github.com/drewnoakes/metadata-extractor
		Metadata metadata;
		try {
			metadata = ImageMetadataReader.readMetadata(fichero);

			for (Directory directory : metadata.getDirectories()) {
				for (Tag tag : directory.getTags()) {

					if (modoDebug) {
						System.out.format("[%s] - %s = %s\n", directory.getName(), tag.getTagName(),
								tag.getDescription());
					}

					if (tag.getTagName().toLowerCase().equals("date/time")
							|| tag.getTagName().toLowerCase().equals("date/time original")) {
						String description = tag.getDescription();
						if (description != null && description.length() >= 4) {
							out = Integer.valueOf(description.substring(0, 4));
						}
					}
				}
//				if (directory.hasErrors()) {
//					for (String error : directory.getErrors()) {
//						System.err.format("ERROR: %s\n", error);
//					}
//				}
			}

		} catch (ImageProcessingException | IOException e) {
			e.printStackTrace();
			System.err.println(
					"Al mirar los metadatos, ha habido excepcion, pero la ejecucion SIGUE. Fichero problematico: "
							+ pathFicheroOrigen);
		}

		if (out == null) {
			System.err.println("Incluso mirando metadatos, no se ha podido deducir ANIO de: " + pathFicheroOrigen);
		}

		return out;
	}

	/**
	 * Devuelve TRUE si el string cumple el formato indicado
	 * 
	 * @param param
	 * @return
	 */
	public static boolean checkFormatoYYYYMMDDHHMMSS(String param) {

		boolean bool = true;
		if (param == null || "".equals(param.trim())) {
			return false;
		}
		try {
			formato_YYYYMMDD_HHMMSS.parse(param);

			int anio = Integer.valueOf(param.substring(0, 0 + 4));
			int mes = Integer.valueOf(param.substring(4, 4 + 2));
			int dia = Integer.valueOf(param.substring(6, 6 + 2));

			int hora = Integer.valueOf(param.substring(8, 8 + 2));
			int minuto = Integer.valueOf(param.substring(10, 10 + 2));
			int segundo = Integer.valueOf(param.substring(12, 12 + 2));

			if (anio < 1975 || anio > 2050 || mes < 1 || mes > 12 || dia < 1 || dia > 31) {
				bool = false;
			}

			if (hora < 1 || hora > 23 || minuto < 1 || minuto > 59 || segundo < 1 || segundo > 59) {
				bool = false;
			}

		} catch (ParseException e) {
			bool = false;
		}
		return bool;
	}

	/**
	 * Devuelve TRUE si el string cumple el formato indicado
	 * 
	 * @param param
	 * @return
	 */
	public static boolean checkFormatoYYYYMMDD(String param) {

		boolean bool = true;
		if (param == null || "".equals(param.trim())) {
			return false;
		}
		try {
			formato_YYYYMMDD.parse(param);

			int anio = Integer.valueOf(param.substring(0, 0 + 4));
			int mes = Integer.valueOf(param.substring(4, 4 + 2));
			int dia = Integer.valueOf(param.substring(6, 6 + 2));

			if (anio < 1975 || anio > 2050 || mes < 1 || mes > 12 || dia < 1 || dia > 31) {
				bool = false;
			}

		} catch (ParseException e) {
			bool = false;
		}
		return bool;
	}

	/**
	 * Revisa todos los ficheros en las carpetas ANUALIZADAS.
	 * 
	 * @param anioInicio
	 * @param anioFin
	 * @return Mapa clave-valor. Clave=[anio, checksum, tamanio],
	 *         valor=path_existente
	 * @throws IOException
	 */
	public static Map<String, String> revisarCarpetasDestinoAnualizadas(String dirPrefijo, Integer anioInicio,
			Integer anioFin) throws IOException {

		System.out.println("============ CARPETAS DESTINO (preexistentes) ==============");

		Map<String, String> mapaMultianioOut = new HashMap<String, String>();

		for (Integer anio = anioInicio; anio <= anioFin; anio++) {

			System.out.println("---- CARPETA DESTINO anio=" + anio + " ----");
			Long contador = 0L;

			String dirCarpetaAnio = dirPrefijo + anio.toString() + "\\";
			File file = new File(dirCarpetaAnio);

			if (Files.notExists(Paths.get(dirCarpetaAnio))) {
				Files.createDirectory(Paths.get(dirCarpetaAnio));
			}

			List<String> listaFicheros = new ArrayList<String>();
			fetchFiles(file, f -> listaFicheros.add(f.getAbsolutePath()));
			System.out.println("Los ficheros ya existentes en la carpeta anualizada del anio=" + anio + " son: "
					+ listaFicheros.size());

			System.out.println("Calculando clave-valor...");
			for (String path_file : listaFicheros) {
				contador++;
				if (contador % LOG_INCREMENTO == 0) {
					System.out.println(contador + "...");
				}

				// CLAVE (ANIO_CHECKSUM_TAMANHO) y VALOR (path en carpeta anualizada)
				mapaMultianioOut.put(calcularClave(anio, path_file), path_file);
			}

		}

		return mapaMultianioOut;

	}

	/**
	 * @param anio
	 * @param pathFichero
	 * @return
	 */
	public static String calcularClave(Integer anio, String pathFichero) {
		long cs = getChecksumValue(new CRC32(), pathFichero);
		long tamanioBytes = FileUtils.sizeOf(new File(pathFichero));
		return new String(anio + "_" + cs + "_" + tamanioBytes);
	}

}

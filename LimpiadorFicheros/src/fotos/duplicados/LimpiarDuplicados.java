/**
 * 
 */
package fotos.duplicados;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

	public static final String PATH_BASE = "D:\\DATOS Y DOCUMENTOS\\IMAGENES_20190101\\NO_ANIMALES";

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

		// ---------- Borrar duplicados
		eliminarListaFicheros(candidatoDuplicados);
	}

	/**
	 * @param dir
	 * @param fileConsumer
	 */
	public static void fetchFiles(File dir, Consumer<File> fileConsumer) {

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
	 * Saca una lista de los duplicados (que son los que borraré)
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
}

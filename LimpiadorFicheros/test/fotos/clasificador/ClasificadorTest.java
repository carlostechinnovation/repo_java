package fotos.clasificador;

import java.io.IOException;

import org.junit.Before;
import org.junit.jupiter.api.Test;

import com.drew.imaging.ImageProcessingException;

class ClasificadorTest {

	Clasificador modelo = null;

	@Before
	void iniciar() {
		modelo = new Clasificador();
	}

	@Test
	void testDeducirAniodeFicheroOrigen() throws ImageProcessingException, IOException {

		String pathFicheroOrigen = "D:\\Imágenes\\NO_ANIMALES\\PENDIENTES\\PENDIENTES_VOLCADO_DISCO_AZUL\\PENDIENTE_ORDENAR_20150801\\100_7777.MOV";
		Integer rangoInicio = 2010;
		Integer rangoFin = 2022;

		Integer out = modelo.deducirAniodeFicheroOrigen(pathFicheroOrigen, rangoInicio, rangoFin, true);
		System.out.println("out=" + out);

		assert (out == 2012);
	}

}

package controladoresselenium.ordenador;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.sikuli.script.FindFailed;

/**
 * App con Selenium para navegacion automatica. CASO 01
 *
 */
public class NavegarAuto extends Thread {

	public static void main(String[] args)
			throws AWTException, InterruptedException, FindFailed, FileNotFoundException {

		System.out.println("Cargando datos importantes desde fichero...");
		String URL_PAGINA1 = (new Scanner(new File("C:\\DATOS\\navega1_NO_BORRAR\\url.txt"))).nextLine();
		String datoUsuario = (new Scanner(new File("C:\\DATOS\\navega1_NO_BORRAR\\usuario.txt"))).nextLine();
		String datoClaveFuera = (new Scanner(new File("C:\\DATOS\\navega1_NO_BORRAR\\clavefuera.txt"))).nextLine();
		String datoClaveDentro = (new Scanner(new File("C:\\DATOS\\navega1_NO_BORRAR\\claveDentro.txt"))).nextLine();

		System.out.println("Configurando propiedades de navegador IExplorer...");
		System.setProperty("webdriver.ie.driver", "C:\\WebDriver\\bin\\IEDriverServer.exe");

		// OPCIONES DEL NAVEGADOR
		InternetExplorerOptions ieo = new InternetExplorerOptions();
		ieo.setPageLoadStrategy(PageLoadStrategy.NORMAL);
		ieo.ignoreZoomSettings();

		ieo.setCapability("nativeEvents", false);
		ieo.setCapability("unexpectedAlertBehaviour", "accept");
		ieo.setCapability("ignoreProtectedModeSettings", true);
		ieo.setCapability("disable-popup-blocking", true);
		ieo.setCapability("enablePersistentHover", true);
		ieo.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);// ACEPTAR CERTIFICADO!!

		// -------------------------------------
		System.out.println("Abriendo navegador IExplorer...");
		WebDriver driver = new InternetExplorerDriver(ieo);// DRIVER
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS); // Implicit wait: 10 segundos
		driver.manage().window().maximize();// MAXIMIZAR ventana
		driver.get(URL_PAGINA1);// Abrir pagina 1

		// BOTON AZUL
		List<WebElement> botones = driver.findElements(By.className("AEAT_boton_main"));
		WebElement botonElegido = botones.get(2); // tercer boton (indice=2)

		driver.manage().timeouts().pageLoadTimeout(5, TimeUnit.SECONDS);
		botonElegido.click();// hacer click

		sleep(1000L);

		// -----------POP UP de Windows certificado -----------------
		System.out.println("POP UP de Windows certificado (robot 1)...");
		Robot robot1 = new Robot(); // Actúa fuera de Selenium

		// Tabulador
		robot1.keyPress(KeyEvent.VK_TAB);
		robot1.keyRelease(KeyEvent.VK_TAB);

		// Enter
		robot1.keyPress(KeyEvent.VK_ENTER);
		robot1.keyRelease(KeyEvent.VK_ENTER);

		sleep(3000L);

		// ------- PANTALLA NEGRA---------------------
		System.out.println("PANTALLA NEGRA...");

		WebElement cajaUsuarioPantallaNegra = driver.findElement(By.id("Enter user name"));
		WebElement cajaPasswdPantallaNegra = driver.findElement(By.id("passwd"));
		WebElement botonPantallaNegra = driver.findElement(By.id("Log_On"));
		cajaUsuarioPantallaNegra.click();
		cajaUsuarioPantallaNegra.sendKeys(datoUsuario);
		cajaPasswdPantallaNegra.click();
		cajaPasswdPantallaNegra.sendKeys(datoClaveFuera);
		botonPantallaNegra.click();

		// ------- PANTALLA INTRANET---------------------
		System.out.println("PANTALLA INTRANET...");
		sleep(3000L);

		// https://stackoverflow.com/questions/9588827/how-to-switch-to-the-new-browser-window-which-opens-after-click-on-the-button
		String winHandleBefore = driver.getWindowHandle();
		for (String winHandle : driver.getWindowHandles()) {
			driver.switchTo().window(winHandle);
		}
		driver.switchTo().window(winHandleBefore);

		WebElement cajaUsuarioPantallaIntranet = driver.findElement(By.id("username"));
		WebElement cajaPasswdPantallaIntranet = driver.findElement(By.id("password"));
		WebElement botonPantallaIntranet = driver.findElement(By.id("loginBtn"));

		cajaUsuarioPantallaIntranet.click();
		cajaUsuarioPantallaIntranet.sendKeys(datoUsuario);
		cajaPasswdPantallaIntranet.click();
		cajaPasswdPantallaIntranet.sendKeys(datoClaveDentro);
		botonPantallaIntranet.click();

		// ----------- BOTONES GRANDES CON IMAGEN---------------
		System.out.println("BOTONES GRANDES CON IMAGEN...");
		sleep(1000L);

		String handlePantallaBotonesGrandes = driver.getWindowHandle();
		System.out.println("Guardamos el HANDLE de esta pantalla para volver luego: " + handlePantallaBotonesGrandes);

		WebElement botonArribaTodasApps = driver.findElement(By.id("allAppsBtn"));
		botonArribaTodasApps.click();
		sleep(1000L);
		List<WebElement> botonesTipoImagenGrande = driver.findElements(By.className("storeapp-icon"));
		WebElement botonDeseado = botonesTipoImagenGrande.get(4);// el quinto boton
		botonDeseado.click();

		sleep(1000L);

		// ------------ Abrir enlace (aviso alerta: "Abrir" o "Guardar Como")
		// ---------------------------
		System.out.println("ABRIENDO el fichero .ica que establecera la conexion...");

		// Tabuladores
		for (int i = 1; i <= 8; i++) {
			System.out.println("Tabulador numero " + i);
			robot1.keyPress(KeyEvent.VK_TAB);
			robot1.keyRelease(KeyEvent.VK_TAB);
			sleep(1000L);
		}

		// Enter
		robot1.keyPress(KeyEvent.VK_ENTER);
		robot1.keyRelease(KeyEvent.VK_ENTER);
		Long esperaLarga = 40 * 1000L;
		System.out.println("Esperando " + esperaLarga + " milisegundos...");
		sleep(esperaLarga);

		// -----------POP UP de Windows certificado -----------------
		System.out.println("POP UP de Windows certificado (pantalla 1 - Host remoto)...");

		Robot miRobot2 = new Robot(); // Actúa fuera de Selenium

		// 2 Tabuladores
		for (int i = 1; i <= 2; i++) {
			miRobot2.keyPress(KeyEvent.VK_TAB);
			miRobot2.keyRelease(KeyEvent.VK_TAB);
			sleep(1000L);
		}

		// Enter
		miRobot2.keyPress(KeyEvent.VK_ENTER);
		miRobot2.keyRelease(KeyEvent.VK_ENTER);

		sleep(4000L);

		// --------------------------------------
		System.out.println("POP UP de Windows certificado (pantalla 2 - Escribir clave)...");
		Robot miRobot3 = new Robot(); // Actúa fuera de Selenium

		StringSelection stringSelection = new StringSelection(datoClaveDentro); // CLAVE INTERNA
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, stringSelection);

		// Pegar (Control-V)
		Robot robot = new Robot();
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_V);
		robot.keyRelease(KeyEvent.VK_V);
		robot.keyRelease(KeyEvent.VK_CONTROL);

		// Enter
		miRobot2.keyPress(KeyEvent.VK_ENTER);
		miRobot2.keyRelease(KeyEvent.VK_ENTER);

		sleep(4000L);

		// --------------------------------------
		// --------------------------------------
		System.out.println(
				"Volvemos a poner el foco en la pantalla de botones GRANDES y tabulamos hasta llegar a la Intranet de Movilidad (para fichar)...");

		System.out.println("Usando este Handle: " + handlePantallaBotonesGrandes);
		driver.switchTo().window(handlePantallaBotonesGrandes);

		// 15 Tabuladores
		System.out.println("Pulsamos muchos tabuladores hasta llegar al boton gordo deseado...");
		for (int i = 1; i <= 15; i++) {
			miRobot2.keyPress(KeyEvent.VK_TAB);
			miRobot2.keyRelease(KeyEvent.VK_TAB);
			sleep(1000L);
		}

		System.out.println("TEMPORAL Sleep gigante para hacerlo a mano...");
		sleep(4000000L);

		// Enter
		miRobot2.keyPress(KeyEvent.VK_ENTER);
		miRobot2.keyRelease(KeyEvent.VK_ENTER);

		sleep(4000L);
		// --------------------------------------

		// Salir
		System.out.println("Cerrando...");
//		driver.quit();

	}

}

package logica;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.time.LocalTime;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import interfaz.InterfazCliente;

public class Cliente {

//	public static final String SERVIDOR = "3.82.26.85";
//	public static final String SERVIDOR = "3.80.255.231";
//	public static final String SERVIDOR = "25.5.99.233";
	public static final String SERVIDOR = "localhost";
	public static final int PUERTO = 8080;
	public static final String INICIO = " [CLIENTE] ";
	public static final String RUTA_LOG = "./data/logs/";
	public static final String RUTA_DOWN = "./data/descargas/";
	public static final String PREPARADO = "PREPARADO";
	public static final String RECIBIDO = "RECIBIDO";
	public static final String LLEGO = "LLEGO";
	public static final String ERROR = "ERROR";
	public static final String FINARCH = "FINARCH$";
	public static final String NOMBRE = "NOMBRE$";
	public static final String SEP = "$";
	public static final int BIEN = 1;
	public static final int MAL = -1;	
	
	
	private String log;
	
	private Socket socket;
	
	private PrintWriter out;
	
	private BufferedReader in;

	private InterfazCliente interfaz;
	
	private String nombreArchivo;
	
	private long numPaquetes;
	
	private long tam;
	
	private long tiempo;
	
	private int estado;
	
	public Cliente(InterfazCliente i) throws Exception {
		interfaz = i;
		nombreArchivo = "Ninguno";
		numPaquetes = 0;
		tam = 0;
		tiempo = 0;
//		blobsArchivo = new ArrayList<>();
		
//		log = "";
//		escribirEnLog("Cliente inicializado, conectando con el servidor . . .\n");
		log = "[" + LocalTime.now() + "]" + INICIO + "Cliente inicializado, conectando con el servidor . . .\n";
		socket = new Socket(SERVIDOR, PUERTO);
//		escribirEnLog("Conexión exitosa con el servidor " + SERVIDOR + ":" + PUERTO);
		log += "[" + LocalTime.now() + "]" + INICIO + "Conexión exitosa con el servidor " + SERVIDOR + ":" + PUERTO;
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
		estado = BIEN;
	}
	
	public int comunicarse() throws Exception {
		try {
			escribirEnLog("Enviando mensaje de " + PREPARADO + " al servidor");
			out.println(PREPARADO);
			escribirEnLog("Mensaje enviado al servidor");
			
			// Esperando la recepción del nombre del archivo a descargar
			String nombre = in.readLine();

			System.out.println("ACA ALGO1");
			if(nombre.contains(NOMBRE)) {
				String n = nombre.replace(NOMBRE, "");
				nombreArchivo = n;
				escribirEnLog("Nombre del archivo a descargar recibido --> " + n);
			}
			else {
				estado = MAL;
				escribirEnLog("ERROR :: Llegó un mensaje que no debía llegar " + nombre);
				cerrar();
			}
			
			// Iniciando recepción y escritura del archivo
			String rutaDesc = RUTA_DOWN + nombreArchivo;
			escribirEnLog("Iniciando recepción y escribiendo el archivo en la ruta " + rutaDesc + " . . .");
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			File f = new File(rutaDesc);
			FileOutputStream fos = new FileOutputStream(f);
			if(!f.exists())
				f.createNewFile();
			byte[] buffer = new byte[8192];
			MessageDigest hashing = MessageDigest.getInstance("SHA-256");
			

			System.out.println("ACA ALGO2");
			
			// Contabilizando el tiempo inicial
			long ini = System.currentTimeMillis();
			int r;
			
			// Recibiendo paquetes del archivo a descargar
			long tamTotal = dis.readLong();
			System.out.println(tamTotal);
			while (tam < tamTotal && (r = dis.read(buffer)) != -1 ) 
			{
				out.println(LLEGO);
				fos.write(buffer, 0, r);
				hashing.update(buffer, 0, r);
				numPaquetes++;
				tam += (r);
				escribirEnLog("Paquete Recibido! tamaño: " + (r) + " bytes");
			}
			fos.flush(); // Por si acaso algo queda en el buffer de escritura
			fos.close();
			

			// Contabilizando el tiempo final
			long fin = System.currentTimeMillis();
			tiempo = (fin - ini)/1000;
			escribirEnLog("Escritura del archivo exitosa !");
			escribirEnLog("Finalizó el envío del archivo. El tiempo total fue de " + tiempo + " segundos");
			escribirEnLog("Número total de paquetes recibidos: " + numPaquetes + " paquetes");
			escribirEnLog("Tamaño total del archivo recibido: " + (tam/(1024.0*1024.0)) + " MiBytes");
			
			// Guardando el archivo en local
//			createFile(rutaDesc);
			out.println("FIN");
			// Verificación de integridad con el hash
//			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(rutaDesc));
//			byte[] completo = new byte[(int)(new File(rutaDesc)).length()];
//			bis.read(completo);
//			bis.close();
			
			escribirEnLog("Iniciando la Validación de integridad . . . ");
//			String hash = new String(blob);
			String hash = in.readLine();
			System.out.println("///");
			if(hash.contains(FINARCH)) 
			{
				
//				String h = hash.split(SEP)[1];
				String h = hash.replace(FINARCH, "");
				escribirEnLog("Hash Recibido del servidor --> " + h);
				
//				MessageDigest hashing = MessageDigest.getInstance("SHA-256");
//				hashing.update(completo);
				byte[] fileHashed = hashing.digest();
				String created = DatatypeConverter.printHexBinary(fileHashed);
				
				escribirEnLog("Hash generado ! --> " + created);
				
				if(created.equals(h)) {
					escribirEnLog("El Archivo se verificó y ESTÁ ÍNTEGRO ! :D");
					escribirEnLog("Se le envía al servidor confirmación: " + RECIBIDO);
					out.println(RECIBIDO);
					// Cierre de los canales
					escribirEnLog("Mensaje de confimación exitosa enviado correctamente :D !");
					escribirEnLog("Cerrando conexión satisfactoriamente . . .");
					dis.close();
					cerrar();
				}
				else {
					estado = MAL;
					escribirEnLog("El archivo SE CORROMPIÓ :( tiene errores porque los hashes no coinciden");
					escribirEnLog("Se le envía al servidor mensaje de error: " + ERROR);
					out.println(ERROR);
					// Cierre de los canales
					escribirEnLog("Mensaje de error enviado correctamente :(");
					escribirEnLog("Cerrando conexión por finalización de proceso erróneo :( . . .");
					dis.close();
					cerrar();
				}
			}
			else 
			{
				estado = MAL;
				escribirEnLog("ERROR :: Llegó un mensaje que no debía llegar " + hash);
				dis.close();
				cerrar();
			}
		}
		catch(Exception e) {
			estado = MAL;
			escribirEnLog("ERROR :: Ocurrió algún error inesperado: " + e.getMessage());
			e.printStackTrace();
			try {
				cerrar();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return estado;
	}
	
	public void escribirEnLog(String mensaje) {
		this.log += "\n[" + LocalTime.now() + "]" + INICIO + mensaje;
		if(interfaz != null) interfaz.actualizar();
	}
	public String getLog() {
		return log;
	}
	public String getNombreArchivo() {
		return nombreArchivo;
	}
	public String getTam() {
		return String.valueOf((tam/(1024.0*1024.0)));
	}
	public String getNumPaquetes() {
		return String.valueOf(numPaquetes);
	}
	public String getTiempo() {
		return String.valueOf(tiempo);
	}
	
	private void cerrar() throws IOException {
		// Se debe primero escribir el log
		File f = new File(RUTA_LOG + (new Date()).toString().replace(":", ".") + " - Registro.log");
		escribirEnLog("Registrando todo en archivo .log local . . .");
		FileOutputStream fos = new FileOutputStream(f, true);
		if(!f.exists())
			f.createNewFile();
	    fos.write(log.getBytes());
	    fos.close();
		escribirEnLog("Log generado ! Hasta la próxima :D !");
		// Posteriormente se cierran los canales 
	    in.close();
		out.close();
		socket.close();
	}
}

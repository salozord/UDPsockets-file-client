package logica;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
	
	private DatagramSocket socket;
	
	private InetAddress address;
	
	private byte[] buf;

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
		socket = new DatagramSocket();
		address = InetAddress.getByName(SERVIDOR);
//		escribirEnLog("Conexi�n exitosa con el servidor " + SERVIDOR + ":" + PUERTO);
		log += "[" + LocalTime.now() + "]" + INICIO + "Conexi�n exitosa con el servidor " + SERVIDOR + ":" + PUERTO;
		estado = BIEN;
	}
	
	public int comunicarse() throws Exception {
		try {
			escribirEnLog("Enviando mensaje de " + PREPARADO + " al servidor");
			
			buf = PREPARADO.getBytes();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, address, PUERTO);
			socket.send(packet);
			
			escribirEnLog("Mensaje enviado al servidor");
			
			buf= new byte[200];
			// Esperando la recepci�n del nombre del archivo a descargar
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			String nombre = new String(packet.getData(), 0, packet.getLength());
			
			System.out.println(nombre);
			if(nombre.contains(NOMBRE)) {
				String n = nombre.replace(NOMBRE, "");
				nombreArchivo = n;
				escribirEnLog("Nombre del archivo a descargar recibido --> " + n);
			}
			else {
				estado = MAL;
				escribirEnLog("ERROR :: Lleg� un mensaje que no deb�a llegar " + nombre);
				cerrar();
			}
			
			// Iniciando recepci�n y escritura del archivo
			String rutaDesc = RUTA_DOWN + nombreArchivo;
			escribirEnLog("Iniciando recepci�n y escribiendo el archivo en la ruta " + rutaDesc + " . . .");

			File f = new File(rutaDesc);
			FileOutputStream fos = new FileOutputStream(f);
			if(!f.exists())
				f.createNewFile();
			MessageDigest hashing = MessageDigest.getInstance("SHA-256");
			

			System.out.println("ACA ALGO2");
			
			// Contabilizando el tiempo inicial
			long ini = System.currentTimeMillis();
			
			// Recibiendo paquetes del archivo a descargar
			buf = new byte[32768];
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			Long tamTotal = Long.parseLong(new String(packet.getData(), 0, packet.getLength()));
			
			System.out.println(tamTotal);
			String hash = "";
			
			while (numPaquetes<tamTotal) 
			{
				packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				hash = new String(packet.getData(), 0, packet.getLength());
				if(hash.contains(FINARCH)) break;
				numPaquetes++;
				
				fos.write(packet.getData(), 0, packet.getData().length);
				hashing.update(packet.getData(), 0, packet.getLength());
				tam += (packet.getData().length);
				escribirEnLog("Paquete Recibido! tama�o: " + (packet.getData().length) + " bytes");
			}
			System.out.println(numPaquetes);
			
			fos.flush(); // Por si acaso algo queda en el buffer de escritura
			fos.close();
			

			// Contabilizando el tiempo final
			long fin = System.currentTimeMillis();
			tiempo = (fin - ini)/1000;
			escribirEnLog("Escritura del archivo exitosa !");
			escribirEnLog("Finaliz� el env�o del archivo. El tiempo total fue de " + tiempo + " segundos");
			escribirEnLog("N�mero total de paquetes recibidos: " + numPaquetes + " paquetes");
			escribirEnLog("Tama�o total del archivo recibido: " + (tam/(1024.0*1024.0)) + " MiBytes");
			
			// Guardando el archivo en local
//			createFile(rutaDesc);
			
			// Verificaci�n de integridad con el hash
//			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(rutaDesc));
//			byte[] completo = new byte[(int)(new File(rutaDesc)).length()];
//			bis.read(completo);
//			bis.close();
			
			escribirEnLog("Iniciando la Validaci�n de integridad . . . ");
//			String hash = new String(blob);
			
			System.out.println(hash);
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
					escribirEnLog("El Archivo se verific� y EST� �NTEGRO ! :D");
					escribirEnLog("Se le env�a al servidor confirmaci�n: " + RECIBIDO);
					buf = RECIBIDO.getBytes();
					packet = new DatagramPacket(buf, buf.length, address, PUERTO);
					socket.send(packet);
					// Cierre de los canales
					escribirEnLog("Mensaje de confimaci�n exitosa enviado correctamente :D !");
					escribirEnLog("Cerrando conexi�n satisfactoriamente . . .");
					cerrar();
				}
				else {
					estado = MAL;
					escribirEnLog("El archivo SE CORROMPI� :( tiene errores porque los hashes no coinciden");
					escribirEnLog("Se le env�a al servidor mensaje de error: " + ERROR);
					buf = ERROR.getBytes();
					packet = new DatagramPacket(buf, buf.length, address, PUERTO);
					socket.send(packet);
					// Cierre de los canales
					escribirEnLog("Mensaje de error enviado correctamente :(");
					escribirEnLog("Cerrando conexi�n por finalizaci�n de proceso err�neo :( . . .");
					cerrar();
				}
			}
			else 
			{
				estado = MAL;
				escribirEnLog("ERROR :: Lleg� un mensaje que no deb�a llegar " + hash);
				cerrar();
			}
		}
		catch(Exception e) {
			estado = MAL;
			escribirEnLog("ERROR :: Ocurri� alg�n error inesperado: " + e.getMessage());
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
		escribirEnLog("Log generado ! Hasta la pr�xima :D !");
		// Posteriormente se cierran los canales 
		socket.close();
	}
}

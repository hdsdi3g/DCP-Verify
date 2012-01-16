/*
 * This file is part of DCPVerify
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2012
 * 
*/

package hd3gtv.dcpverify;

import hd3gtv.tools.XmlData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;

import org.w3c.dom.Element;

public class DCPverify {
	
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		File dcpdir = new File(args[0]);
		
		assetmapParser(new File(dcpdir + File.separator + "ASSETMAP.xml"));
		
		File[] dcpfiles = dcpdir.listFiles();
		for (int pos = 0; pos < dcpfiles.length; pos++) {
			if (dcpfiles[pos].getName().endsWith("_pkl.xml")) {
				packinglistParser(dcpfiles[pos]);
			}
		}
		
		System.out.println("Done.");
	}
	
	public static byte[] getShaForFile(File file) throws Exception {
		MessageDigest messagedigestinstance = MessageDigest.getInstance("SHA-1");
		FileInputStream fileinputstream = new FileInputStream(file);
		
		messagedigestinstance.reset();
		
		byte[] buffer = new byte[1024 * 1024]; // 1 Mo
		int len;
		
		while ((len = fileinputstream.read(buffer)) > 0) {
			messagedigestinstance.update(buffer, 0, len);
		}
		return messagedigestinstance.digest();
	}
	
	public static void assetmapParser(File assetmap) throws IOException {
		System.out.println("[AssetMap]");
		XmlData xml = XmlData.loadFromFile(assetmap);
		
		Element e_assetlist = XmlData.getElementByName(xml.getDocumentElement(), "AssetList");
		ArrayList<Element> assets = XmlData.getElementsByName(e_assetlist, "Asset");
		
		Element e_chunk;
		Element e_path;
		Element e_length;
		File asset;
		Long asset_length;
		for (int pos = 0; pos < assets.size(); pos++) {
			e_chunk = XmlData.getElementByName(XmlData.getElementByName(assets.get(pos), "ChunkList"), "Chunk");
			e_path = XmlData.getElementByName(e_chunk, "Path");
			e_length = XmlData.getElementByName(e_chunk, "Length");
			asset = new File(assetmap.getParent() + File.separator + e_path.getTextContent());
			asset_length = Long.valueOf(e_length.getTextContent());
			
			if (asset.exists()) {
				if (asset.length() == asset_length) {
					System.out.println("Assetmap/Asset " + asset.getName() + " has expected size (" + String.valueOf(asset_length) + ")");
				} else {
					System.err.println("Assetmap/Asset " + asset.getName() + " don't has expected size (" + String.valueOf(asset_length) + ") instead of (" + asset.length() + ")");
				}
			} else {
				System.err.println("Assetmap/Asset " + asset.getName() + " don't exist");
			}
			
		}
		/*
		<AssetMap xmlns="http://www.smpte-ra.org/schemas/429-9/2007/AM">
		  [...]
		  <AssetList>
		    <Asset>
		      [...]
		      <ChunkList>
		        <Chunk>
		          <Path>26fce990-c68c-494f-aafa-cf968c24e35f_pkl.xml</Path>
		          [...]
		          <Length>1133</Length>
		*/
	}
	
	public static void packinglistParser(File packinglist) throws Exception {
		System.out.println("[PackingList]");
		
		XmlData xml = XmlData.loadFromFile(packinglist);
		
		Element e_assetlist = XmlData.getElementByName(xml.getDocumentElement(), "AssetList");
		ArrayList<Element> assets = XmlData.getElementsByName(e_assetlist, "Asset");
		
		Element e_annotationtext;
		Element e_hash;
		Element e_size;
		File asset;
		Long asset_size;
		String realhash;
		String expectedhash;
		for (int pos = 0; pos < assets.size(); pos++) {
			
			e_annotationtext = XmlData.getElementByName(assets.get(pos), "AnnotationText");
			if (e_annotationtext == null) {
				continue;
			}
			
			e_hash = XmlData.getElementByName(assets.get(pos), "Hash");
			e_size = XmlData.getElementByName(assets.get(pos), "Size");
			
			asset = new File(packinglist.getParent() + File.separator + e_annotationtext.getTextContent());
			asset_size = Long.valueOf(e_size.getTextContent());
			
			if (asset.exists()) {
				if (asset.length() == asset_size) {
					System.out.println("Assetmap/Asset " + asset.getName() + " has expected size (" + String.valueOf(asset_size) + ")");
				} else {
					System.err.println("Assetmap/Asset " + asset.getName() + " don't has expected size (" + String.valueOf(asset_size) + ") instead of (" + asset.length() + ")");
				}
			} else {
				System.err.println("Assetmap/Asset " + asset.getName() + " don't exist");
			}
			
			expectedhash = byteToString(Base64.decode(e_hash.getTextContent()));
			System.out.println(" start hash computing...");
			realhash = byteToString(getShaForFile(asset));
			
			if (expectedhash.equals(realhash)) {
				System.out.println("Assetmap/Asset " + asset.getName() + " has expected hash (" + realhash + ")");
			} else {
				System.err.println("Assetmap/Asset " + asset.getName() + " don't has expected hash (" + realhash + ") instead of (" + expectedhash + ")");
			}
			
			/*
			<PackingList xmlns="http://www.smpte-ra.org/schemas/429-8/2007/PKL" xmlns:dsig="http://www.w3.org/2000/09/xmldsig#">
			[...]
			<AssetList>
			<Asset>
			  [...]
			  <AnnotationText>TESTMELAKA-VID.mxf</AnnotationText>
			  <Hash>tNzHoQGC+ErTdVUm0LMrpATFXT0=</Hash>
			  <Size>866624400</Size>
			  [...]
			
			*/
		}
	}
	
	private static String byteToString(byte[] b) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			int v = b[i] & 0xFF;
			if (v < 16) {
				sb.append(0);
			}
			sb.append(Integer.toString(v, 16).toLowerCase());
		}
		return sb.toString();
	}
	
}

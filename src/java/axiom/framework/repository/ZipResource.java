/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile: ZipResource.java,v $
 * $Author: hannes $
 * $Revision: 1.7 $
 * $Date: 2005/12/19 22:15:11 $
 */

package axiom.framework.repository;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ZipResource implements Resource {

    private String entryName;
    private ZipRepository repository;
    private String name;
    private String shortName;
    private String baseName;

    protected ZipResource(String zipentryName, ZipRepository repository) {
        this.entryName = zipentryName;
        this.repository = repository;

        int lastSlash = entryName.lastIndexOf('/');

        shortName = entryName.substring(lastSlash + 1);
        name = new StringBuffer(repository.getName()).append('/')
                .append(shortName).toString();

        // base name is short name with extension cut off
        int lastDot = shortName.lastIndexOf(".");
        baseName = (lastDot == -1) ? shortName : shortName.substring(0, lastDot);
    }

    public long lastModified() {
        return repository.lastModified();
    }

    public InputStream getInputStream() throws IOException {
        ZipFile zipfile = null;
        try {
            zipfile = repository.getZipFile();
            ZipEntry entry = zipfile.getEntry(entryName);
            if (entry == null) {
                throw new IOException("Zip resource " + this + " does not exist");
            }
            int size = (int) entry.getSize();
            byte[] buf = new byte[size];
            InputStream in = zipfile.getInputStream(entry);
            int read = 0;
            while (read < size) {
                int r = in.read(buf, read, size-read);
                if (r == -1)
                    break;
                read += r;
            }
            in.close();
            return new ByteArrayInputStream(buf);
        } finally {
            zipfile.close();
        }
    }

    public boolean exists() {
        ZipFile zipfile = null;
        try {
            zipfile = repository.getZipFile();
            return (zipfile.getEntry(entryName) != null);
        } catch (Exception ex) {
            return false;
        } finally {
            try {
                zipfile.close();
            } catch (Exception ex) {}
        }
    }

    public String getContent(String encoding) throws IOException {
        ZipFile zipfile = null;
        try {
            zipfile = repository.getZipFile();
            ZipEntry entry = zipfile.getEntry(entryName);
            if (entry == null) {
                throw new IOException("Zip resource " + this + " does not exist");
            }
            InputStream in = zipfile.getInputStream(entry);
            int size = (int) entry.getSize();
            byte[] buf = new byte[size];
            int read = 0;
            while (read < size) {
                int r = in.read(buf, read, size-read);
                if (r == -1)
                    break;
                read += r;
            }
            in.close();
            return encoding == null ?
                    new String(buf) :
                    new String(buf, encoding);
        } finally {
            if (zipfile != null) {
                zipfile.close();
            }
        }
    }

    public String getContent() throws IOException {
        return getContent(null);
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getBaseName() {
        return baseName;
    }

    public URL getUrl() {
        // TODO: we might want to return a Jar URL
        // http://java.sun.com/j2se/1.5.0/docs/api/java/net/JarURLConnection.html
        throw new UnsupportedOperationException("getUrl() not implemented for ZipResource");
    }

    public long getLength() {
        ZipFile zipfile = null;
        try {
            zipfile = repository.getZipFile();
            return zipfile.getEntry(entryName).getSize();            
        } catch (Exception ex) {
            return 0;
        } finally {
            try {
                zipfile.close();
            } catch (Exception ex) {}
        }
    }

    public Repository getRepository() {
        return repository;
    }

    public int hashCode() {
        return 17 + name.hashCode();
    }

    public boolean equals(Object obj) {
        return obj instanceof ZipResource && name.equals(((ZipResource) obj).name);
    }

    public String toString() {
        return getName();
    }
}

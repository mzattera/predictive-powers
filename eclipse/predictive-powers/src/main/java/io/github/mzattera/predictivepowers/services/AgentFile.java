package io.github.mzattera.predictivepowers.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public abstract class AgentFile {
    
    // Factory method to create the appropriate RemoteFile instance
    public static AgentFile create(String descriptor) {
        if (descriptor.startsWith("http://") || descriptor.startsWith("https://")) {
            return new RemoteFileURL(descriptor);
        } else if (descriptor.startsWith("/")) {
            return new RemoteFileLocal(descriptor);
        } else {
            return new RemoteFileServer(descriptor);
        }
    }

    // Common methods for all RemoteFile types
    public abstract void createFile() throws IOException;
    public abstract void changeFile() throws IOException;
    public abstract void deleteFile() throws IOException;
    public abstract InputStream getInputStream() throws IOException;
}

// Local file implementation
class RemoteFileLocal extends AgentFile {
    private File file;

    public RemoteFileLocal(String path) {
        this.file = new File(path);
    }

    @Override
    public void createFile() throws IOException {
        // Implementation for creating a local file
    }

    @Override
    public void changeFile() throws IOException {
        // Implementation for changing a local file
    }

    @Override
    public void deleteFile() throws IOException {
        // Implementation for deleting a local file
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }
}

// URL implementation
class RemoteFileURL extends AgentFile {
    private String url;

    public RemoteFileURL(String url) {
        this.url = url;
    }

    @Override
    public void createFile() throws IOException {
        // Not applicable for URL
    }

    @Override
    public void changeFile() throws IOException {
        // Not applicable for URL
    }

    @Override
    public void deleteFile() throws IOException {
        // Not applicable for URL
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new URL(url).openStream();
    }
}

// Server file implementation
class RemoteFileServer extends AgentFile {
    private String fileId;

    public RemoteFileServer(String fileId) {
        this.fileId = fileId;
    }

    @Override
    public void createFile() throws IOException {
    	throw new UnsupportedOperationException();
    }

    @Override
    public void changeFile() throws IOException {
    	throw new UnsupportedOperationException();
    }

    @Override
    public void deleteFile() throws IOException {
    	throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() throws IOException {
    	throw new UnsupportedOperationException();
    }
}

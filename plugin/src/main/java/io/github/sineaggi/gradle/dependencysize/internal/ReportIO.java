package io.github.sineaggi.gradle.dependencysize.internal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

final class ReportIO {
    private ReportIO() {}

    static void write(ReportData report, OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);
        data.writeUTF(report.getProjectPath());
        List<HolderData> holders = report.getHoldersList();
        data.writeInt(holders.size());
        for (HolderData holder : holders) {
            data.writeUTF(holder.getConfigurationName());
            data.writeUTF(holder.getPath());
            data.writeUTF(holder.getGav());
            data.writeUTF(holder.getGroup());
            data.writeUTF(holder.getArtifact());
            data.writeUTF(holder.getVersion());
            data.writeLong(holder.getSize());
        }
        data.flush();
    }

    static ReportData read(InputStream in) throws IOException {
        DataInputStream data = new DataInputStream(in);
        String projectPath = data.readUTF();
        int count = data.readInt();
        List<HolderData> holders = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String configurationName = data.readUTF();
            String path = data.readUTF();
            String gav = data.readUTF();
            String group = data.readUTF();
            String artifact = data.readUTF();
            String version = data.readUTF();
            long size = data.readLong();
            holders.add(new HolderData(configurationName, path, size, gav, group, artifact, version));
        }
        return new ReportData(projectPath, holders);
    }
}

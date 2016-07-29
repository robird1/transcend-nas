package com.transcend.nas.settings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ikelee on 16/07/20.
 */
public class DiskStructDevice {
    public final static List<String> FORMAT = Arrays.asList("sector", "cylinder", "sectorsize", "psectorsize", "head", "connectiontype",
            "length", "external", "path", "model", "type", "serial", "partition");
    public Map<String, String> infos = new HashMap<String, String>();
    public List<DiskStructPartition> partitions;
    public DiskStructRAID raid;
    public float totalSize = 0;
    public float availableSize = 0;
    public float blockSize = 0;
}

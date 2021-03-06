/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.regionserver;

import org.apache.hadoop.hbase.shaded.com.google.common.base.Function;
import org.apache.hadoop.hbase.shaded.com.google.common.collect.ImmutableList;
import org.apache.hadoop.hbase.shaded.com.google.common.collect.Ordering;

import java.util.Comparator;

import org.apache.yetus.audience.InterfaceAudience;

/**
 * Useful comparators for comparing StoreFiles.
 */
@InterfaceAudience.Private
final class StoreFileComparators {
  /**
   * Comparator that compares based on the Sequence Ids of the the StoreFiles. Bulk loads that did
   * not request a seq ID are given a seq id of -1; thus, they are placed before all non- bulk
   * loads, and bulk loads with sequence Id. Among these files, the size is used to determine the
   * ordering, then bulkLoadTime. If there are ties, the path name is used as a tie-breaker.
   */
  public static final Comparator<StoreFile> SEQ_ID =
      Ordering.compound(ImmutableList.of(Ordering.natural().onResultOf(new GetSeqId()),
        Ordering.natural().onResultOf(new GetFileSize()).reverse(),
        Ordering.natural().onResultOf(new GetBulkTime()),
        Ordering.natural().onResultOf(new GetPathName())));

  /**
   * Comparator for time-aware compaction. SeqId is still the first ordering criterion to maintain
   * MVCC.
   */
  public static final Comparator<StoreFile> SEQ_ID_MAX_TIMESTAMP =
      Ordering.compound(ImmutableList.of(Ordering.natural().onResultOf(new GetSeqId()),
        Ordering.natural().onResultOf(new GetMaxTimestamp()),
        Ordering.natural().onResultOf(new GetFileSize()).reverse(),
        Ordering.natural().onResultOf(new GetBulkTime()),
        Ordering.natural().onResultOf(new GetPathName())));

  private static class GetSeqId implements Function<StoreFile, Long> {
    @Override
    public Long apply(StoreFile sf) {
      return sf.getMaxSequenceId();
    }
  }

  private static class GetFileSize implements Function<StoreFile, Long> {
    @Override
    public Long apply(StoreFile sf) {
      if (sf.getReader() != null) {
        return sf.getReader().length();
      } else {
        // the reader may be null for the compacted files and if the archiving
        // had failed.
        return -1L;
      }
    }
  }

  private static class GetBulkTime implements Function<StoreFile, Long> {
    @Override
    public Long apply(StoreFile sf) {
      return sf.getBulkLoadTimestamp().orElse(Long.MAX_VALUE);
    }
  }

  private static class GetPathName implements Function<StoreFile, String> {
    @Override
    public String apply(StoreFile sf) {
      return sf.getPath().getName();
    }
  }

  private static class GetMaxTimestamp implements Function<StoreFile, Long> {
    @Override
    public Long apply(StoreFile sf) {
      return sf.getMaximumTimestamp().orElse(Long.MAX_VALUE);
    }
  }
}

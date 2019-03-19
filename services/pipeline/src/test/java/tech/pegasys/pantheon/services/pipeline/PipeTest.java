/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.services.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import tech.pegasys.pantheon.metrics.Counter;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PipeTest {
  private final Counter inputCounter = mock(Counter.class);
  private final Counter outputCounter = mock(Counter.class);
  private final Pipe<String> pipe = new Pipe<>(5, inputCounter, outputCounter);

  @Test
  public void shouldNotHaveMoreWhenEmptyAndClosed() {
    pipe.close();
    assertThat(pipe.hasMore()).isFalse();
  }

  @Test
  public void shouldHaveMoreWhenNotEmptyAndClosed() {
    pipe.put("A");
    pipe.close();

    assertThat(pipe.hasMore()).isTrue();

    pipe.get();

    assertThat(pipe.hasMore()).isFalse();
  }

  @Test
  public void shouldNotHaveMoreWhenAbortedEvenIfNotEmpty() {
    pipe.put("A");
    pipe.abort();

    assertThat(pipe.hasMore()).isFalse();
  }

  @Test
  public void shouldLimitNumberOfItemsDrained() {
    pipe.put("a");
    pipe.put("b");
    pipe.put("c");
    pipe.put("d");

    final List<String> output = new ArrayList<>();
    pipe.drainTo(output, 3);
    assertThat(output).containsExactly("a", "b", "c");
  }

  @Test
  public void shouldNotWaitToReachMaximumSizeBeforeReturningBatch() {
    pipe.put("a");
    final List<String> output = new ArrayList<>();
    pipe.drainTo(output, 3);
    assertThat(output).containsExactly("a");
  }

  @Test
  public void shouldNotBeOpenAfterAbort() {
    pipe.abort();
    assertThat(pipe.isOpen()).isFalse();
  }

  @Test
  public void shouldIncrementInputCounterWhenItemAddedToPipe() {
    pipe.put("A");
    verify(inputCounter).inc();
    pipe.put("B");
    verify(inputCounter, times(2)).inc();
  }

  @Test
  public void shouldIncrementOutputCounterWhenItemRemovedToPipeWithPoll() {
    pipe.put("A");
    pipe.put("B");
    pipe.poll();
    verify(outputCounter).inc();

    pipe.poll();
    verify(outputCounter, times(2)).inc();

    assertThat(pipe.poll()).isNull();
    verify(outputCounter, times(2)).inc();
  }

  @Test
  public void shouldIncrementOutputCounterWhenItemRemovedToPipeWithGet() {
    pipe.put("A");
    pipe.put("B");
    pipe.close();
    pipe.get();
    verify(outputCounter).inc();

    pipe.get();
    verify(outputCounter, times(2)).inc();

    assertThat(pipe.get()).isNull();
    verify(outputCounter, times(2)).inc();
  }
}
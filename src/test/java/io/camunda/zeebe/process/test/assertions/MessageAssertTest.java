package io.camunda.zeebe.process.test.assertions;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.extensions.ZeebeProcessTest;
import io.camunda.zeebe.process.test.testengine.InMemoryEngine;
import io.camunda.zeebe.process.test.util.Utilities;
import io.camunda.zeebe.process.test.util.Utilities.ProcessPackMessageEvent;
import io.camunda.zeebe.process.test.util.Utilities.ProcessPackMessageStartEvent;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@ZeebeProcessTest
class MessageAssertTest {

  public static final String CORRELATION_KEY = "correlationkey";
  public static final String WRONG_CORRELATION_KEY = "wrongcorrelationkey";
  public static final String WRONG_MESSAGE_NAME = "wrongmessagename";

  @Nested
  class HappyPathTests {

    private ZeebeClient client;
    private InMemoryEngine engine;

    @RepeatedTest(10)
    void testHasBeenCorrelated() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      Utilities.startProcessInstance(engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      BpmnAssert.assertThat(response).hasBeenCorrelated();
    }

    @RepeatedTest(10)
    void testHasMessageStartEventBeenCorrelated() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine,
              client,
              ProcessPackMessageStartEvent.MESSAGE_NAME,
              ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      BpmnAssert.assertThat(response).hasCreatedProcessInstance();
    }

    @RepeatedTest(10)
    void testHasNotBeenCorrelated() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      BpmnAssert.assertThat(response).hasNotBeenCorrelated();
    }

    @RepeatedTest(10)
    void testHasMessageStartEventNotBeenCorrelated() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, WRONG_MESSAGE_NAME, ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      BpmnAssert.assertThat(response).hasNotCreatedProcessInstance();
    }

    @RepeatedTest(10)
    void testHasExpired() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Duration timeToLive = Duration.ofDays(1);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY, timeToLive);
      Utilities.increaseTime(engine, timeToLive.plusMinutes(1));

      // then
      BpmnAssert.assertThat(response).hasExpired();
    }

    @RepeatedTest(10)
    void testHasNotExpired() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      BpmnAssert.assertThat(response).hasNotExpired();
    }

    @RepeatedTest(10)
    void testExtractingProcessInstance() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      Utilities.startProcessInstance(engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      BpmnAssert.assertThat(response).extractingProcessInstance().isCompleted();
    }

    @RepeatedTest(10)
    void testExtractingProcessInstance_messageStartEvent() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine,
              client,
              ProcessPackMessageStartEvent.MESSAGE_NAME,
              ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      BpmnAssert.assertThat(response).extractingProcessInstance().isCompleted();
    }
  }

  @Nested
  class UnhappyPathTests {

    private ZeebeClient client;
    private InMemoryEngine engine;

    @RepeatedTest(10)
    void testHasBeenCorrelatedFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).hasBeenCorrelated())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Message with key %d was not correlated", response.getMessageKey());
    }

    @RepeatedTest(10)
    void testHasMessageStartEventBeenCorrelatedFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, WRONG_MESSAGE_NAME, ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).hasCreatedProcessInstance())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Message with key %d did not lead to the creation of a process instance",
              response.getMessageKey());
    }

    @RepeatedTest(10)
    void testHasNotBeenCorrelatedFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).hasNotBeenCorrelated())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Message with key %d was correlated to process instance %s",
              response.getMessageKey(), instanceEvent.getProcessInstanceKey());
    }

    @RepeatedTest(10)
    void testHasMessageStartEventNotBeenCorrelatedFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine,
              client,
              ProcessPackMessageStartEvent.MESSAGE_NAME,
              ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).hasNotCreatedProcessInstance())
          .isInstanceOf(AssertionError.class)
          .hasMessageContaining(
              "Message with key %d was correlated to process instance", response.getMessageKey());
    }

    @RepeatedTest(10)
    void testHasExpiredFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).hasExpired())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Message with key %d has not expired", response.getMessageKey());
    }

    @RepeatedTest(10)
    void testHasNotExpiredFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Duration timeToLive = Duration.ofDays(1);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY, timeToLive);
      Utilities.increaseTime(engine, timeToLive.plusMinutes(1));

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).hasNotExpired())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Message with key %d has expired", response.getMessageKey());
    }

    @RepeatedTest(10)
    void testExtractingProcessInstanceFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      Utilities.startProcessInstance(engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, WRONG_CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).extractingProcessInstance())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected to find one correlated process instance for message key %d but found %d: %s",
              response.getMessageKey(), 0, "[]");
    }

    @RepeatedTest(10)
    void testExtractingProcessInstanceFailure_messageStartEvent() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, WRONG_MESSAGE_NAME, ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).extractingProcessInstance())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected to find one correlated process instance for message key %d but found %d: %s",
              response.getMessageKey(), 0, "[]");
    }
  }
}

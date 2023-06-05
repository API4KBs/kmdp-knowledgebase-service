package edu.mayo.kmdp.language.common.cmmn;

import edu.mayo.kmdp.util.StreamUtil;
import java.util.stream.Stream;
import javax.xml.bind.JAXBElement;
import org.omg.spec.cmmn._20151109.model.TCase;
import org.omg.spec.cmmn._20151109.model.TCaseFileItem;
import org.omg.spec.cmmn._20151109.model.TCaseFileItemDefinition;
import org.omg.spec.cmmn._20151109.model.TCaseFileItemOnPart;
import org.omg.spec.cmmn._20151109.model.TCriterion;
import org.omg.spec.cmmn._20151109.model.TDefinitions;
import org.omg.spec.cmmn._20151109.model.TDiscretionaryItem;
import org.omg.spec.cmmn._20151109.model.TPlanItem;
import org.omg.spec.cmmn._20151109.model.TPlanItemDefinition;
import org.omg.spec.cmmn._20151109.model.TPlanItemOnPart;
import org.omg.spec.cmmn._20151109.model.TPlanningTable;
import org.omg.spec.cmmn._20151109.model.TSentry;
import org.omg.spec.cmmn._20151109.model.TStage;
import org.omg.spec.cmmn._20151109.model.TTask;

public class CMMN11Utils {

  private CMMN11Utils() {
    // functions only
  }

  /**
   * returns the Case Model's stages, recursively
   *
   * @param sourceModel the Case Model
   * @return a Stream of the Case Model's {@link TStage}
   */
  public static Stream<TStage> streamStages(TDefinitions sourceModel) {
    return sourceModel.getCase().stream()
        .map(TCase::getCasePlanModel)
        .flatMap(CMMN11Utils::streamStages);
  }

  /**
   * returns the Case Model's tasks, recursively
   *
   * @param sourceModel the Case Model
   * @return a Stream of the Case Model's {@link TTask}
   */
  public static Stream<TTask> streamTasks(TDefinitions sourceModel) {
    return streamStages(sourceModel)
        .flatMap(st -> st.getPlanItemDefinition().stream())
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TTask.class));
  }

  /**
   * returns the Case Model's tasks, recursively
   *
   * @param sourceModel the Case Model
   * @param type        a class filter to restrict the returned Tasks by type
   * @return a Stream of the Case Model's {@link TTask}
   */
  public static <K extends TTask> Stream<K> streamTasks(
      TDefinitions sourceModel,
      Class<K> type) {
    return streamTasks(sourceModel)
        .flatMap(StreamUtil.filterAs(type));
  }


  /**
   * returns the Case Model's definition elements (e.g. tasks, stages) that feed into an On-part
   * Entry Sentry, recursively
   *
   * @param sourceModel the Case Model
   * @return a Stream of the Case Model's {@link TPlanItemDefinition} that feed into an On-part
   * Entry Sentry
   */
  public static Stream<TPlanItemDefinition> streamOnSentrySources(TDefinitions sourceModel) {
    return streamStages(sourceModel)
        .flatMap(stage -> stage.getSentry().stream())
        .flatMap(s -> s.getOnPart().stream())
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TPlanItemOnPart.class))
        .map(TPlanItemOnPart::getSourceRef)
        .flatMap(StreamUtil.filterAs(TPlanItem.class))
        .map(TPlanItem::getDefinitionRef)
        .flatMap(StreamUtil.filterAs(TPlanItemDefinition.class));
  }

  /**
   * returns the Case Model's definition elements (e.g. tasks, stages) that have an On-part Entry
   * Sentry, recursively
   *
   * @param sourceModel the Case Model
   * @return a Stream of the Case Model's {@link TPlanItemDefinition} that have an On-part Entry
   * Sentry
   */
  public static Stream<TPlanItemDefinition> streamOnSentryTargets(TDefinitions sourceModel) {
    return streamStages(sourceModel)
        .flatMap(stage -> stage.getPlanItem().stream())
        .filter(pi -> pi.getEntryCriterion().stream()
            .map(TCriterion::getSentryRef)
            .flatMap(StreamUtil.filterAs(TSentry.class))
            .flatMap(s -> s.getOnPart().stream())
            .map(JAXBElement::getValue)
            .flatMap(StreamUtil.filterAs(TPlanItemOnPart.class))
            .map(TPlanItemOnPart::getSourceRef)
            .flatMap(StreamUtil.filterAs(TPlanItem.class))
            .findAny().isPresent())
        .map(TPlanItem::getDefinitionRef)
        .flatMap(StreamUtil.filterAs(TPlanItemDefinition.class));
  }

  /**
   * returns the Case Model's definition elements (e.g. tasks, stages) that have an On-part Entry
   * Sentry with a CaseFileItem-driven If-part recursively
   *
   * @param sourceModel the Case Model
   * @return a Stream of the Case Model's {@link TPlanItemDefinition} that have an On-part Entry
   * Sentry with a CaseFileItem-driven If-part recursively
   */
  public static Stream<TPlanItemDefinition> streamOnDataSentryTargets(TDefinitions sourceModel) {
    return streamStages(sourceModel)
        .flatMap(stage -> stage.getPlanItem().stream())
        .filter(pi -> pi.getEntryCriterion().stream()
            .map(TCriterion::getSentryRef)
            .flatMap(StreamUtil.filterAs(TSentry.class))
            .flatMap(s -> s.getOnPart().stream())
            .map(JAXBElement::getValue)
            .flatMap(StreamUtil.filterAs(TCaseFileItemOnPart.class))
            .map(TCaseFileItemOnPart::getSourceRef)
            .flatMap(StreamUtil.filterAs(TCaseFileItem.class))
            .findAny().isPresent())
        .map(TPlanItem::getDefinitionRef)
        .flatMap(StreamUtil.filterAs(TPlanItemDefinition.class));
  }


  /**
   * returns the Case Model's CaseFileItem elements (e.g. tasks, stages)
   *
   * @param sourceModel the Case Model
   * @return a Stream of the Case Model's {@link TCaseFileItem}
   */
  public static Stream<TCaseFileItemDefinition> streamCaseFileItemDefs(TDefinitions sourceModel) {
    return sourceModel.getCaseFileItemDefinition().stream();
  }


  /**
   * returns a Stage's nested Stages, recursively
   *
   * @param root the root Stage
   * @return a Stream of the root {@link TStage}, plus any nested Stages
   */
  private static Stream<TStage> streamStages(TStage root) {
    return Stream.concat(
        Stream.of(root),
        root.getPlanItemDefinition().stream()
            .map(JAXBElement::getValue)
            .flatMap(StreamUtil.filterAs(TStage.class))
            .flatMap(CMMN11Utils::streamStages));
  }

  /**
   * returns a Cases's Discretionary Items, from nested Stages PlanningTables, recursively
   *
   * @param root the root model
   * @return a Stream of the case models' {@link TDiscretionaryItem}, recursively
   */
  public static Stream<TDiscretionaryItem> streamDiscretionaryItems(TDefinitions root) {
    return streamStages(root)
        .flatMap(st -> streamDiscretionaryItems(st.getPlanningTable()));
  }

  private static Stream<TDiscretionaryItem> streamDiscretionaryItems(TPlanningTable table) {
    if (table == null) {
      return Stream.empty();
    }
    return table.getTableItem().stream()
        .map(JAXBElement::getValue)
        .flatMap(item -> {
          if (item instanceof TDiscretionaryItem) {
            return Stream.of((TDiscretionaryItem) item);
          } else if (item instanceof TPlanningTable) {
            return streamDiscretionaryItems((TPlanningTable) item);
          } else {
            return Stream.empty();
          }
        });
  }


}


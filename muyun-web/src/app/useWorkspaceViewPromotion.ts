import { computed, type MaybeRefOrGetter, toValue } from 'vue';
import type { DrawerPromotion } from '@muyun/platform-components';
import { useWorkbenchNavigation } from './workbenchNavigation';
import {
  createWorkspaceViewDescriptor,
  type WorkspaceViewDefinition,
  type WorkspaceViewInput,
} from './workspaceViews';

/** Facts required by the source host; dirty state belongs to the shared view session. */
export interface WorkspaceViewPromotionEligibility {
  hasStableIdentity: boolean;
  busy?: boolean;
  /** A legacy host may decline until its detail state has moved into a view session. */
  canChangeHost?: boolean;
}

export function canPromoteWorkspaceView(eligibility: WorkspaceViewPromotionEligibility) {
  return eligibility.hasStableIdentity && eligibility.busy !== true && eligibility.canChangeHost !== false;
}

export function useWorkspaceViewPromotion<TInput extends WorkspaceViewInput>(options: {
  view: WorkspaceViewDefinition<TInput>;
  input: MaybeRefOrGetter<TInput | undefined>;
  /** A source host can provide its resolved business identity for the new tab label. */
  title?: MaybeRefOrGetter<string | undefined>;
  eligibility: MaybeRefOrGetter<WorkspaceViewPromotionEligibility>;
  /** Runs only when the destination host will mount and consume the hand-off. */
  beforePromote?: (input: TInput) => void;
  onPromoted?: (result: { created: boolean }) => void;
}) {
  const navigation = useWorkbenchNavigation();
  return computed<DrawerPromotion | undefined>(() => {
    const input = toValue(options.input);
    if (!input || !canPromoteWorkspaceView(toValue(options.eligibility)) || !navigation) return undefined;
    return {
      title: '固定为页签',
      promote: () => {
        const result = navigation.openPage(
          createWorkspaceViewDescriptor(options.view, input, 'tab', toValue(options.title)),
        );
        if (result.created) options.beforePromote?.(input);
        options.onPromoted?.(result);
      },
    };
  });
}

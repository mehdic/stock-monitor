# Coordination System

This directory is used for autonomous agent-to-agent communication.

## Structure

- `task_proposals.json` - Task proposals from planner agent
- `active_work_registry.json` - Currently active work by agents
- `completed_work_log.json` - Log of completed tasks
- `planned_work_queue.json` - Queue of planned work
- `messages/` - Agent-to-agent messages
- `agent_locks/` - Lock files for conflict prevention
- `logs/` - Activity and notification logs

## Agent Communication Protocol

1. **Planner Agent**: Creates task proposals with multiple approaches
2. **Reviewer Agent**: Evaluates proposals and chooses best approach
3. **Planner Agent**: Implements chosen approach after approval
4. Both agents log their activities for transparency

## Monitoring

Watch real-time updates:
```bash
# Watch task proposals
watch -n 2 "cat coordination/task_proposals.json | jq ."

# Watch notifications
tail -f coordination/logs/notifications.log

# Watch all agent activity
tail -f coordination/logs/agent_activity.log
```

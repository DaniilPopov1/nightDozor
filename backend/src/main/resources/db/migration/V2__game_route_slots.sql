ALTER TABLE public.games
    ADD COLUMN route_slots_count integer NOT NULL DEFAULT 1;

ALTER TABLE public.games
    ADD CONSTRAINT chk_games_route_slots_count_positive CHECK (route_slots_count >= 1);

ALTER TABLE public.team_game_routes
    RENAME COLUMN team_id TO assigned_team_id;

ALTER TABLE public.team_game_routes
    ALTER COLUMN assigned_team_id DROP NOT NULL;

ALTER TABLE public.team_game_routes
    ADD COLUMN slot_number integer;

WITH numbered_routes AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY game_id ORDER BY created_at, id) AS generated_slot_number
    FROM public.team_game_routes
)
UPDATE public.team_game_routes route
SET slot_number = numbered_routes.generated_slot_number
FROM numbered_routes
WHERE route.id = numbered_routes.id;

ALTER TABLE public.team_game_routes
    ALTER COLUMN slot_number SET NOT NULL;

ALTER TABLE public.team_game_routes
    ADD CONSTRAINT chk_team_game_routes_slot_number_positive CHECK (slot_number >= 1);

ALTER TABLE public.team_game_routes
    DROP CONSTRAINT IF EXISTS uk_team_game_routes_game_team;

ALTER TABLE public.team_game_routes
    ADD CONSTRAINT uk_team_game_routes_game_slot UNIQUE (game_id, slot_number);

CREATE UNIQUE INDEX IF NOT EXISTS uk_team_game_routes_game_assigned_team
    ON public.team_game_routes (game_id, assigned_team_id)
    WHERE assigned_team_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_team_game_routes_assigned_team_id
    ON public.team_game_routes (assigned_team_id);

CREATE INDEX IF NOT EXISTS idx_team_game_routes_slot_number
    ON public.team_game_routes (slot_number);

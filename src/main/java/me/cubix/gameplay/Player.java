package me.cubix.gameplay;

import me.cubix.world.World;
import me.cubix.world.block.BlockId;
import org.joml.Vector3f;

public final class Player {

    public final Vector3f vel = new Vector3f();

    // 튜닝 값
    public float gravity = 100f;
    public float jumpSpeed = 20f;


    // 발바닥 기준 위치
    public final Vector3f pos = new Vector3f();

    // 지면 판정
    public boolean onGround = false;

    // 몸통 크기 (원하면 옵션/상수로 조절)
    public float halfWidth = 0.30f;   // 좌우 반폭 (0.3이면 폭 0.6)
    // height는 moveAndCollide 인자로 받음(= 옵션 playerHeight로 쓰기 쉬움)

    private static final float EPS = 1e-5f;

    /**
     * 카메라가 이동하려던 delta를 플레이어 몸(AABB)에 적용하되,
     * 월드 블록과 충돌하면 밀어내며 이동한다.
     *
     * @param world  월드(메뉴에선 null일 수 있음)
     * @param delta  이번 프레임에 "가고 싶었던" 이동량 (x,y,z)
     * @param height 플레이어 AABB 높이(발바닥~머리). 옵션 playerHeight 그대로 써도 됨.
     */
    public void moveAndCollide(World world, Vector3f delta, float height) {
        if (world == null) {
            pos.add(delta);
            onGround = false;
            return;
        }

        onGround = false;

        // 1) X
        if (delta.x != 0f) {
            pos.x = moveAxis(world, pos.x, pos.y, pos.z, delta.x, 0, height);
        }

        // 2) Z
        if (delta.z != 0f) {
            pos.z = moveAxis(world, pos.x, pos.y, pos.z, delta.z, 2, height);
        }

        // 3) Y (중력/점프가 있으면 보통 마지막)
        if (delta.y != 0f) {
            float oldY = pos.y;
            pos.y = moveAxis(world, pos.x, pos.y, pos.z, delta.y, 1, height);

            // 아래로 이동하려다 막혔으면 지면
            if (delta.y < 0f && pos.y > oldY - Math.abs(delta.y) + EPS) {
                // 위 조건은 좀 애매할 수 있어서, 더 확실하게:
                // "실제로 덜 내려갔다"를 체크하는 방식이 안정적임.
            }
        }

        // Y 지면판정은 "아래로 움직였는데 막힘"으로 잡는 게 제일 깔끔함
        if (delta.y < 0f) {
            // 아래로 움직이려 했는데 충돌로 인해 델타만큼 못 내려갔다면 onGround
            // (pos.y는 이미 moveAxis에서 보정되었음)
            float attempted = pos.y + delta.y; // 원래라면 여기까지 가야 함(개념상)
            // 실제 pos.y는 attempted보다 크면(덜 내려갔으면) 바닥에 걸린 것
            if (pos.y > attempted + EPS) onGround = true;
        }
    }

    /**
     * axis: 0=x, 1=y, 2=z
     */
    private float moveAxis(World world, float x, float y, float z, float d, int axis, float height) {
        if (d == 0f) return (axis == 0 ? x : axis == 1 ? y : z);

        float nx = x, ny = y, nz = z;
        if (axis == 0) nx += d;
        if (axis == 1) ny += d;
        if (axis == 2) nz += d;

        // 이동 후 AABB
        float minX = nx - halfWidth;
        float maxX = nx + halfWidth;
        float minY = ny;
        float maxY = ny + height;
        float minZ = nz - halfWidth;
        float maxZ = nz + halfWidth;

        // 겹치는 블록 범위(정수좌표)
        int bx0 = fastFloor(minX);
        int bx1 = fastFloor(maxX - EPS);
        int by0 = fastFloor(minY);
        int by1 = fastFloor(maxY - EPS);
        int bz0 = fastFloor(minZ);
        int bz1 = fastFloor(maxZ - EPS);

        // 충돌 해결: 이동 방향 쪽에서 "가장 가까운 벽"으로 클램프
        if (axis == 0) {
            if (d > 0f) {
                // +X로 이동: 부딪히는 블록의 x면(bx)에서 멈춤
                float allowedX = nx;
                for (int bx = bx0; bx <= bx1; bx++) {
                    // 플레이어가 +X로 이동이면, 실제로 중요한 건 "오른쪽 면" 근처 블록들
                    // 하지만 단순 스캔으로도 충분히 빠름(청크 단위 최적화는 나중에)
                    for (int by = by0; by <= by1; by++) {
                        for (int bz = bz0; bz <= bz1; bz++) {
                            if (!isSolid(world, bx, by, bz)) continue;
                            // 이 블록의 왼쪽 면은 x = bx
                            float stop = bx - halfWidth - EPS;
                            if (stop < allowedX) allowedX = stop;
                        }
                    }
                }
                // nx는 "목표"였고, allowedX는 "최대 허용"
                if (allowedX < nx) nx = allowedX;
            } else {
                // -X로 이동: 부딪히는 블록의 x면(bx+1)에서 멈춤
                float allowedX = nx;
                for (int bx = bx0; bx <= bx1; bx++) {
                    for (int by = by0; by <= by1; by++) {
                        for (int bz = bz0; bz <= bz1; bz++) {
                            if (!isSolid(world, bx, by, bz)) continue;
                            // 이 블록의 오른쪽 면은 x = bx + 1
                            float stop = (bx + 1) + halfWidth + EPS;
                            if (stop > allowedX) allowedX = stop;
                        }
                    }
                }
                if (allowedX > nx) nx = allowedX;
            }
            return nx;
        }

        if (axis == 2) {
            if (d > 0f) {
                float allowedZ = nz;
                for (int bx = bx0; bx <= bx1; bx++) {
                    for (int by = by0; by <= by1; by++) {
                        for (int bz = bz0; bz <= bz1; bz++) {
                            if (!isSolid(world, bx, by, bz)) continue;
                            float stop = bz - halfWidth - EPS;
                            if (stop < allowedZ) allowedZ = stop;
                        }
                    }
                }
                if (allowedZ < nz) nz = allowedZ;
            } else {
                float allowedZ = nz;
                for (int bx = bx0; bx <= bx1; bx++) {
                    for (int by = by0; by <= by1; by++) {
                        for (int bz = bz0; bz <= bz1; bz++) {
                            if (!isSolid(world, bx, by, bz)) continue;
                            float stop = (bz + 1) + halfWidth + EPS;
                            if (stop > allowedZ) allowedZ = stop;
                        }
                    }
                }
                if (allowedZ > nz) nz = allowedZ;
            }
            return nz;
        }

        // axis == 1 (Y)
        if (d > 0f) {
            // 위로: 천장에 박으면 y를 낮춤 (블록 바닥면 = by)
            float allowedY = ny;
            for (int bx = bx0; bx <= bx1; bx++) {
                for (int by = by0; by <= by1; by++) {
                    for (int bz = bz0; bz <= bz1; bz++) {
                        if (!isSolid(world, bx, by, bz)) continue;
                        // 이 블록의 바닥은 y = by
                        float stop = by - height - EPS;
                        if (stop < allowedY) allowedY = stop;
                    }
                }
            }
            if (allowedY < ny) ny = allowedY;
        } else {
            // 아래로: 바닥에 걸리면 y를 올림 (블록 윗면 = by+1)
            float allowedY = ny;
            for (int bx = bx0; bx <= bx1; bx++) {
                for (int by = by0; by <= by1; by++) {
                    for (int bz = bz0; bz <= bz1; bz++) {
                        if (!isSolid(world, bx, by, bz)) continue;
                        // 이 블록의 윗면은 y = by + 1
                        float stop = (by + 1) + EPS;
                        if (stop > allowedY) allowedY = stop;
                    }
                }
            }
            if (allowedY > ny) ny = allowedY;
        }
        return ny;
    }

    private boolean isSolid(World world, int x, int y, int z) {
        short id = world.getBlock(x, y, z);
        // 공기/물은 비고체(원하면 나중에 물 충돌 따로 처리)
        return id != BlockId.AIR && id != BlockId.WATER;
    }

    private static int fastFloor(float v) {
        int i = (int) v;
        return (v < i) ? (i - 1) : i;
    }
}

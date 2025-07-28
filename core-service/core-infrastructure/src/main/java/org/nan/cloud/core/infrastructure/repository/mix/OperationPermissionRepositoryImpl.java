package org.nan.cloud.core.infrastructure.repository.mix;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.OperationPermission;
import org.nan.cloud.core.infrastructure.repository.mongo.document.OperationPermissionDocument;
import org.nan.cloud.core.infrastructure.repository.mongo.repository.OperationPermissionMongoRepository;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.OperationPermissionDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.OperationPermissionMapper;
import org.nan.cloud.core.repository.OperationPermissionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class OperationPermissionRepositoryImpl implements OperationPermissionRepository {

    private final OperationPermissionMapper operationPermissionMapper;
    private final OperationPermissionMongoRepository operationPermissionMongoRepository;
    private final CommonConverter commonConverter;

    @Override
    public void insertRoleOperationRel(Long rid, Set<Long> operationPermissionIds) {
        operationPermissionMapper.insertRoleOperationPermissionRel(rid, operationPermissionIds);
    }

    @Override
    public void updateRoleOperationRel(Long rid, Set<Long> operationPermissionIds) {
        operationPermissionMapper.deleteRoleOperationPermissionRel(rid);
        operationPermissionMapper.insertRoleOperationPermissionRel(rid, operationPermissionIds);
    }

    @Override
    public List<OperationPermission> getOperationPermissionByRid(Long rid) {
        List<Long> operationPermissionIds = operationPermissionMapper.getOperationPermissionIdByRid(rid);
        if (CollectionUtils.isEmpty(operationPermissionIds)) return null;
        List<OperationPermissionDO> operationPermissionDOS = operationPermissionMapper.selectByIds(operationPermissionIds);
        List<OperationPermission> operationPermissions = commonConverter.toOperationPermission(operationPermissionDOS);
        Map<Long, OperationPermissionDocument> operationPermissionMap = operationPermissionMongoRepository.getOperationPermissionDocumentsByOpIds(operationPermissions.stream().map(OperationPermission::getOperationPermissionId).toList());
        operationPermissions.forEach(e -> e.setPermissions(operationPermissionMap.get(e.getOperationPermissionId()).getPermissions()));
        return operationPermissions;
    }

    /**
     * 用于获取用户可见操作权限（不包含详细接口权限对应）
     * @param rids
     * @return
     */
    @Override
    public List<OperationPermission> getOperationPermissionByRids(List<Long> rids) {
        List<Long> operationPermissionIds = operationPermissionMapper.getOperationPermissionIdByRids(rids);
        if (CollectionUtils.isEmpty(operationPermissionIds)) return null;
        List<OperationPermissionDO> operationPermissionDOS = operationPermissionMapper.selectByIds(operationPermissionIds);
        return commonConverter.toOperationPermission(operationPermissionDOS);
    }

    @Override
    public List<Long> getOperationPermissionIdByRids(List<Long> rids) {
        return operationPermissionMapper.getOperationPermissionIdByRids(rids);
    }

    @Override
    public Map<Long, String> getPermissionsByOperationPermissionId(Long rid) {
        return operationPermissionMongoRepository.getPermissionsByOperationPermissionId(rid);
    }

    @Override
    public Set<Long> getPermissionIdsByOperationPermissionIds(List<Long> operationPermissionIds) {
        return operationPermissionMongoRepository.getPermissionIdsByOpIds(operationPermissionIds);
    }

    public List<OperationPermission> getAllOperationPermissions() {
        List<OperationPermissionDO> operationPermissionDOS = operationPermissionMapper.selectList(new LambdaQueryWrapper<OperationPermissionDO>());
        return commonConverter.toOperationPermission(operationPermissionDOS);
    }
}

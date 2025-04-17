package org.clubplus.clubplusbackend.TestUnitaire.mock.dao;

import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class MockMembreDao implements MembreDao {
    @Override
    public Optional<Membre> findByEmail(String email) {
        return Optional.empty();
    }

    @Override
    public boolean existsByEmail(String email) {
        return false;
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Integer id) {
        return false;
    }

    @Override
    public Optional<Membre> findAdminByClubId(Integer clubId) {
        return Optional.empty();
    }

    @Override
    public Set<Membre> findMembresByClubIdWithAdhesions(Integer clubId) {
        return Set.of();
    }

    @Override
    public List<Object[]> findMonthlyRegistrationsSince(LocalDate startDate) {
        return List.of();
    }

    @Override
    public List<Membre> findByAdhesionsClubId(Integer clubId) {
        return List.of();
    }

    @Override
    public void flush() {

    }

    @Override
    public <S extends Membre> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public <S extends Membre> List<S> saveAllAndFlush(Iterable<S> entities) {
        return List.of();
    }

    @Override
    public void deleteAllInBatch(Iterable<Membre> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Integer> integers) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public Membre getOne(Integer integer) {
        return null;
    }

    @Override
    public Membre getById(Integer integer) {
        return null;
    }

    @Override
    public Membre getReferenceById(Integer integer) {
        return null;
    }

    @Override
    public <S extends Membre> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends Membre> List<S> findAll(Example<S> example) {
        return List.of();
    }

    @Override
    public <S extends Membre> List<S> findAll(Example<S> example, Sort sort) {
        return List.of();
    }

    @Override
    public <S extends Membre> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends Membre> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends Membre> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends Membre, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public <S extends Membre> S save(S entity) {
        return null;
    }

    @Override
    public <S extends Membre> List<S> saveAll(Iterable<S> entities) {
        return List.of();
    }

    @Override
    public Optional<Membre> findById(Integer integer) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(Integer integer) {
        return false;
    }

    @Override
    public List<Membre> findAll() {
        return List.of();
    }

    @Override
    public List<Membre> findAllById(Iterable<Integer> integers) {
        return List.of();
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(Integer integer) {

    }

    @Override
    public void delete(Membre entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends Integer> integers) {

    }

    @Override
    public void deleteAll(Iterable<? extends Membre> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public List<Membre> findAll(Sort sort) {
        return List.of();
    }

    @Override
    public Page<Membre> findAll(Pageable pageable) {
        return null;
    }
}

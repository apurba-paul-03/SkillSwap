package com.example.skillswap;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SavedSkillDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSavedSkill(SavedSkillEntity skill);

    @Delete
    void deleteSavedSkill(SavedSkillEntity skill);

    @Query("SELECT * FROM saved_skills")
    List<SavedSkillEntity> getAllSavedSkills();

    @Query("SELECT EXISTS(SELECT 1 FROM saved_skills WHERE id = :id)")
    boolean isSkillSaved(String id);
}

/*
 * NewRMarkdownDialog.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownContext;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateData;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class NewRMarkdownDialog extends ModalDialog<NewRMarkdownDialog.Result>
{
   public static class Result
   {  
      public Result(String author, String title, String format, 
                    List<NewRmdFormatOption> options)
      {
         result_ = toJSO(author, title, format, 
                         NewRmdFormatOptions.optionsListToJson(options));
      }
      
      public JavaScriptObject getJSOResult()
      {
         return result_;
      }
      
      private final native JavaScriptObject toJSO(String author, 
                                                 String title, 
                                                 String format, 
                                                 JavaScriptObject options) /*-{
         var output = new Object();
         output[format] = options;
         
         var result = new Object();
         result["author"] = author;
         result["title"] = title;
         result["output"] = output;
        
         return result;
      }-*/;

      private JavaScriptObject result_;
   }

   public interface Binder extends UiBinder<Widget, NewRMarkdownDialog>
   {
   }

   public NewRMarkdownDialog(
         RMarkdownContext context,
         OperationWithInput<Result> operation)
   {
      super("New R Markdown Document", operation);
      context_ = context;
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      listTemplates_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            updateOptions(getSelectedTemplate());
         }
      });
      listFormats_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            updateFormatOptions(getSelectedFormat());
         }
      });

      templates_ = RmdTemplateData.getTemplates();
      for (int i = 0; i < templates_.length(); i++)
      {
         listTemplates_.addItem(templates_.get(i).getName());
      }
      listTemplates_.setSelectedIndex(0);
      updateOptions(getSelectedTemplate());
   }

   @Override
   protected Result collectInput()
   {
      return new Result(txtAuthor_.getText().trim(), txtTitle_.getText().trim(),
            getSelectedFormat(), optionWidgets_);
   }

   @Override
   protected boolean validate(Result input)
   {
      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   private String getSelectedTemplate() 
   {
      return listTemplates_.getItemText(listTemplates_.getSelectedIndex());
   }
   
   private String getSelectedFormat()
   {
      return listFormats_.getValue(listFormats_.getSelectedIndex());
   }
   
   private void updateOptions(String selectedTemplate)
   {
      for (int i = 0; i < templates_.length(); i++)
      {
         if (templates_.get(i).getName().equals(selectedTemplate))
         {
            updateOptions(templates_.get(i));
            break;
         }
      }
   }
   
   private void updateOptions(RmdTemplate template)
   {
      formats_ = template.getFormats();
      options_ = template.getOptions();
      listFormats_.clear();
      for (int i = 0; i < formats_.length(); i++)
      {
         listFormats_.addItem(formats_.get(i).getUiName(), 
                              formats_.get(i).getName());
      }
      mapOptions_ = new HashMap<String, RmdTemplateFormatOption>();
      for (int i = 0; i < options_.length(); i++)
      {
         mapOptions_.put(options_.get(i).getName(), options_.get(i));
      }
      updateFormatOptions(getSelectedFormat());
   }
   
   private void updateFormatOptions(String format)
   {
      panelOptions_.clear();
      for (int i = 0; i < formats_.length(); i++)
      {
         if (formats_.get(i).getName().equals(format))
         {
            addFormatOptions(formats_.get(i));
            break;
         }
      }
   }
   
   private void addFormatOptions(RmdTemplateFormat format)
   {
      optionWidgets_ = new ArrayList<NewRmdFormatOption>();
      JsArrayString options = format.getOptions();
      for (int i = 0; i < options.length(); i++)
      {
         NewRmdFormatOption optionWidget;
         RmdTemplateFormatOption option = mapOptions_.get(options.get(i));
         if (option.getType().equals("boolean"))
         {
            optionWidget = new NewRmdBooleanOption(option);
         } 
         else if (option.getType().equals("choice"))
         {
            optionWidget = new NewRmdChoiceOption(option);
         }
         else
            continue;
         
         optionWidgets_.add(optionWidget);
         panelOptions_.add(optionWidget);
      }
   }
   
   @UiField TextBox txtAuthor_;
   @UiField TextBox txtTitle_;
   @UiField ListBox listTemplates_;
   @UiField ListBox listFormats_;
   @UiField VerticalPanel panelOptions_;

   private final Widget mainWidget_;

   private JsArray<RmdTemplate> templates_;
   private JsArray<RmdTemplateFormat> formats_;
   private JsArray<RmdTemplateFormatOption> options_;
   private Map<String, RmdTemplateFormatOption> mapOptions_;
   private List<NewRmdFormatOption> optionWidgets_;
   
   @SuppressWarnings("unused")
   private final RMarkdownContext context_;
}